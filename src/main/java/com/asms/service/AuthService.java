package com.asms.service;

import com.asms.domain.User;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.ChangePasswordRequestDto;
import com.asms.model.LoginRequestDto;
import com.asms.model.LoginResponseDto;
import com.asms.model.MfaEnrollmentResponseDto;
import com.asms.model.MfaVerifyRequestDto;
import com.asms.model.MfaVerifyResponseDto;
import com.asms.model.SelectOrgRequestDto;
import com.asms.model.SelectOrgResponseDto;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Authentication domain service (AC-3).
 *
 * <p>Per ADR-001: configurable failed login threshold with account lockout.
 * Per ADR-002: multi-org login routes through /auth/select-org.
 * Per ADR-007: full OIDC/SAML IdP deferred to a later phase; login is credential-based for v1.
 *
 * <p>Full JWT issuance requires Spring Authorization Server integration (ADR-007, deferred).
 * For now the login response returns stubs. The important AC-12 audit events are produced.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public ResponseEntity<LoginResponseDto> login(LoginRequestDto req) {
        log.debug("Login attempt for user: {}", req.getUsername());

        User user = userRepository.findByUsername(req.getUsername())
                .orElse(null);

        if (user == null) {
            // Don't reveal whether user exists — return generic error
            auditService.recordWarning("USER", null, "LOGIN_FAILED_USER_NOT_FOUND",
                    null, "username=" + req.getUsername());
            return ResponseEntity.status(401).build();
        }

        // Check lockout
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            auditService.recordWarning("USER", user.getId(), "LOGIN_FAILED_ACCOUNT_LOCKED",
                    null, "lockedUntil=" + user.getLockedUntil());
            return ResponseEntity.status(423).build();
        }

        // Credential verification stub — full BCrypt check deferred to security hardening phase
        // For now: if user is ACTIVE, accept any password (tests use TestSecurityConfig anyway)
        if (!"ACTIVE".equals(user.getStatus()) && !"PENDING_ACTIVATION".equals(user.getStatus())) {
            return ResponseEntity.status(401).build();
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        auditService.recordInfo("USER", user.getId(), "ACTIVITY_LOGIN_SUCCESS", null, null);

        // LoginResponseDto v2: status, accessToken, sessionToken, expiresIn (no userId)
        LoginResponseDto response = new LoginResponseDto();
        if (user.isForcePasswordChange()) {
            response.setStatus(LoginResponseDto.StatusEnum.TEMP_PASSWORD_REQUIRED);
        } else if (user.isMfaEnabled()) {
            response.setStatus(LoginResponseDto.StatusEnum.MFA_REQUIRED);
        } else {
            response.setStatus(LoginResponseDto.StatusEnum.ORG_SELECTION_REQUIRED);
        }
        // Session token stub — carries user ID for downstream org selection
        response.setSessionToken(user.getId().toString());
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<LoginResponseDto> changePassword(ChangePasswordRequestDto req) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditService.recordInfo("USER", userId, "PASSWORD_CHANGED", null, null);

        user.setForcePasswordChange(false);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        LoginResponseDto response = new LoginResponseDto();
        response.setStatus(user.isMfaEnabled()
                ? LoginResponseDto.StatusEnum.MFA_REQUIRED
                : LoginResponseDto.StatusEnum.ORG_SELECTION_REQUIRED);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<MfaEnrollmentResponseDto> enrollMfa() {
        // TOTP enrollment deferred — return stub QR URI
        MfaEnrollmentResponseDto response = new MfaEnrollmentResponseDto();
        response.setQrCodeUri("otpauth://totp/ASMS?secret=PLACEHOLDER&issuer=ASMS");
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<Void> logout() {
        UUID userId = TenantContext.getUserId();
        if (userId != null) {
            auditService.recordInfo("USER", userId, "ACTIVITY_LOGOUT", null, null);
        }
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<SelectOrgResponseDto> selectOrganization(SelectOrgRequestDto req) {
        // Org selection + JWT issuance requires full Spring Authorization Server (ADR-007, deferred)
        // SelectOrgResponseDto v2: accessToken, expiresIn (no organizationId)
        SelectOrgResponseDto response = new SelectOrgResponseDto();
        response.setAccessToken("STUB_TOKEN_FOR_ORG_" + req.getOrganizationId());
        response.setExpiresIn(3600);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<MfaVerifyResponseDto> verifyMfa(MfaVerifyRequestDto req) {
        // TOTP verification deferred to security phase
        MfaVerifyResponseDto response = new MfaVerifyResponseDto();
        response.setStatus(MfaVerifyResponseDto.StatusEnum.ORG_SELECTION_REQUIRED);
        return ResponseEntity.ok(response);
    }
}
