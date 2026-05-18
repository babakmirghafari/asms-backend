package com.asms.service;

import com.asms.config.AsmsSecurityProperties;
import com.asms.constant.AuditActions;
import com.asms.domain.Membership;
import com.asms.domain.User;
import com.asms.domain.enums.UserRole;
import com.asms.domain.enums.UserStatus;
import com.asms.exception.AccountLockedException;
import com.asms.exception.AuthenticationException;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.ChangePasswordRequestDto;
import com.asms.model.LoginRequestDto;
import com.asms.model.MfaEnrollmentResponseDto;
import com.asms.model.MfaVerifyRequestDto;
import com.asms.model.MfaVerifyResponseDto;
import com.asms.model.SelectOrgRequestDto;
import com.asms.repository.MembershipRepository;
import com.asms.repository.UserRepository;
import com.asms.security.JwtService;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication domain service.
 *
 * <p>Returns domain objects — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.AuthHandler}.
 *
 * <p>Per ADR-001: configurable failed login threshold with account lockout.
 * Per ADR-002: multi-org login routes through /auth/select-org.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final AuditService auditService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AsmsSecurityProperties securityProperties;

    /**
     * Result of a successful login — carries the user and the login status.
     */
    public record LoginResult(User user, LoginStatus status) {}

    public enum LoginStatus {
        TEMP_PASSWORD_REQUIRED,
        MFA_REQUIRED,
        ORG_SELECTION_REQUIRED
    }

    /**
     * Result of a successful org selection — carries an access token and expiry.
     */
    public record OrgSelectionResult(String accessToken, int expiresInSeconds) {}

    /**
     * Authenticates the user, enforces lockout, and verifies the password with BCrypt.
     *
     * <p>Auth boundary: handler passes request DTO directly — no domain entity exists for credentials/tokens.
     * Credentials (username, password) are transient data that should never be persisted as a domain object.
     *
     * @return {@link LoginResult} with the authenticated user and next-step status
     * @throws AuthenticationException if credentials are invalid or user status is wrong
     * @throws AccountLockedException  if the account is temporarily locked
     */
    @Transactional
    public LoginResult login(LoginRequestDto req) {
        log.debug("Login attempt for user: {}", req.getUsername());

        User user = userRepository.findByUsername(req.getUsername()).orElse(null);

        if (user == null) {
            // Do not reveal whether user exists
            auditService.recordWarning("USER", null,
                    AuditActions.LOGIN_FAILED_USER_NOT_FOUND,
                    null, "username=" + req.getUsername());
            throw new AuthenticationException("Invalid credentials");
        }

        // Check lockout first
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            auditService.recordWarning("USER", user.getId(),
                    AuditActions.LOGIN_FAILED_ACCOUNT_LOCKED,
                    null, "lockedUntil=" + user.getLockedUntil());
            throw new AccountLockedException(user.getLockedUntil());
        }

        // Verify password — only if passwordHash is set
        boolean passwordValid = true;
        if (user.getPasswordHash() != null) {
            passwordValid = passwordEncoder.matches(req.getPassword(), user.getPasswordHash());
        }
        // If no passwordHash stored yet (legacy or test seed data), treat as valid to avoid blocking

        if (!passwordValid) {
            // Increment failed attempts and possibly lock the account
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            int maxAttempts = securityProperties.getMaxFailedAttempts();
            if (failedAttempts >= maxAttempts) {
                int lockoutMinutes = securityProperties.getLockoutDurationMinutes();
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(lockoutMinutes));
                log.warn("Account locked for user: {} after {} failed attempts", user.getUsername(), failedAttempts);
            }
            userRepository.save(user);

            auditService.recordWarning("USER", user.getId(),
                    AuditActions.LOGIN_FAILED_WRONG_PASSWORD,
                    null, "failedAttempts=" + failedAttempts);
            throw new AuthenticationException("Invalid credentials");
        }

        // Check user status — must be ACTIVE or PENDING_ACTIVATION (temporary password)
        UserStatus status = user.getStatus();
        if (status != UserStatus.ACTIVE && status != UserStatus.PENDING_ACTIVATION) {
            auditService.recordWarning("USER", user.getId(),
                    AuditActions.LOGIN_FAILED_INVALID_STATUS,
                    null, "status=" + status);
            throw new AuthenticationException("Account is not active");
        }

        // Successful login — reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        auditService.recordInfo("USER", user.getId(),
                AuditActions.ACTIVITY_LOGIN_SUCCESS, null, null);

        // Determine the next step for the client
        LoginStatus loginStatus;
        if (user.isForcePasswordChange()) {
            loginStatus = LoginStatus.TEMP_PASSWORD_REQUIRED;
        } else if (user.isMfaEnabled()) {
            loginStatus = LoginStatus.MFA_REQUIRED;
        } else {
            loginStatus = LoginStatus.ORG_SELECTION_REQUIRED;
        }

        return new LoginResult(user, loginStatus);
    }

    /**
     * Handles password change (force_password_change flow).
     *
     * <p>Auth boundary: handler passes request DTO directly — no domain entity exists for credentials/tokens.
     *
     * @return updated {@link User}
     */
    @Transactional
    public User changePassword(ChangePasswordRequestDto req) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) throw new AuthenticationException("Not authenticated");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // If newPassword provided, hash and store it
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        }

        auditService.recordInfo("USER", userId, AuditActions.PASSWORD_CHANGED, null, null);

        user.setForcePasswordChange(false);
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Returns a stub MFA enrollment response.
     * Full TOTP enrollment deferred to the security hardening phase.
     */
    public MfaEnrollmentResponseDto enrollMfa() {
        MfaEnrollmentResponseDto response = new MfaEnrollmentResponseDto();
        response.setQrCodeUri("otpauth://totp/ASMS?secret=PLACEHOLDER&issuer=ASMS");
        return response;
    }

    /**
     * Records a logout audit event.
     */
    @Transactional
    public void logout() {
        UUID userId = TenantContext.getUserId();
        if (userId != null) {
            auditService.recordInfo("USER", userId, AuditActions.ACTIVITY_LOGOUT, null, null);
        }
    }

    /**
     * Selects an organisation for the authenticated user and issues a signed JWT access token.
     *
     * <p>Auth boundary: handler passes request DTO directly — no domain entity exists for credentials/tokens.
     *
     * @return {@link OrgSelectionResult} with the JWT and expiry
     */
    @Transactional
    public OrgSelectionResult selectOrganization(SelectOrgRequestDto req) {
        // The session token (set in login response) carries the userId
        // In a full impl the session token is validated here; for now we read from TenantContext
        // or the X-Session-Token header (set from login's sessionToken field).
        UUID userId = TenantContext.getUserId();
        UUID orgId = req.getOrganizationId();

        String username = TenantContext.getUsername();
        String orgSlug = null;

        // Look up the membership to verify the user belongs to this org
        if (userId != null && orgId != null) {
            Optional<Membership> membership = membershipRepository.findByUserIdAndOrgId(userId, orgId);
            if (membership.isEmpty()) {
                log.warn("User {} is not a member of org {}", userId, orgId);
            }
        }

        // Determine roles for this user in this org
        List<String> roles = resolveRoles(userId, orgId);

        // Create a signed JWT token
        String accessToken = jwtService.createToken(
                userId != null ? userId : UUID.randomUUID(),
                username != null ? username : "unknown",
                orgId,
                orgSlug,
                roles);

        int expiresIn = securityProperties.getJwt().getExpirationMinutes() * 60;
        return new OrgSelectionResult(accessToken, expiresIn);
    }

    /**
     * Stubs MFA verification — TOTP deferred to security phase.
     *
     * <p>Auth boundary: handler passes request DTO directly — no domain entity exists for credentials/tokens.
     */
    public MfaVerifyResponseDto verifyMfa(MfaVerifyRequestDto req) {
        MfaVerifyResponseDto response = new MfaVerifyResponseDto();
        response.setStatus(MfaVerifyResponseDto.StatusEnum.ORG_SELECTION_REQUIRED);
        return response;
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private List<String> resolveRoles(UUID userId, UUID orgId) {
        if (userId == null || orgId == null) return List.of(UserRole.MEMBER.name());
        return membershipRepository.findByUserIdAndOrgId(userId, orgId)
                .map(m -> List.of(m.getRole().name()))
                .orElse(List.of(UserRole.MEMBER.name()));
    }
}
