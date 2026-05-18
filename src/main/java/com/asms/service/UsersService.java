package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.User;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.UserStatus;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateUserRequestDto;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User lifecycle management service (AC-3, AC-12, AC-13).
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.UsersHandler}.
 *
 * <p>All list queries are org-scoped via the memberships table (ADR-006 / RISK-002).
 * Every write produces an audit event with before/after state (ADR-009).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public User createUser(User user) {
//        log.debug("Create user: {}", req.getUsername());
//        User user = User.builder()
//                .username(req.getUsername())
//                .email(req.getEmail())
//                .phoneNumber(req.getPhoneNumber())
//                .status(UserStatus.INACTIVE)
//                .forcePasswordChange(true)
//                .mfaEnabled(false)
//                .failedLoginAttempts(0)
//                .createdAt(OffsetDateTime.now())
//                .updatedAt(OffsetDateTime.now())
//                .build();
        User saved = userRepository.save(user);
        auditService.recordInfo("USER", saved.getId(), AuditActions.USER_CREATED, null, saved);
        return saved;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = loadUser(userId);
        User before = copyForAudit(user);
        user.setStatus(UserStatus.DELETED);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        auditService.recordInfo("USER", userId, AuditActions.USER_DELETED, before, null);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return loadUser(userId);
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(
            Integer page, Integer size, String sort, String search, UUID organizationId) {
        // AC-13: org scope — use param if provided, otherwise TenantContext
        UUID orgId = organizationId != null ? organizationId : TenantContext.getOrgId();
        return userRepository.findAllByOrgId(
                orgId, search,
                UserStatus.DELETED.getKey(), MembershipStatus.ACTIVE.getKey(),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    /**
     * Updates profile fields on an existing user. The handler converts the request DTO to a
     * partial {@link User} carrying only the non-null fields and passes it here.
     *
     * @param userId target user identifier
     * @param patch  partial User carrying fields to update (null fields are not applied)
     */
    @Transactional
    public User updateUser(UUID userId, User patch) {
        User user = loadUser(userId);
        User before = copyForAudit(user);
        if (patch.getFirstName() != null)   user.setFirstName(patch.getFirstName());
        if (patch.getLastName() != null)    user.setLastName(patch.getLastName());
        if (patch.getEmail() != null)       user.setEmail(patch.getEmail());
        if (patch.getPhoneNumber() != null) user.setPhoneNumber(patch.getPhoneNumber());
        user.setUpdatedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);
        auditService.recordInfo("USER", userId, AuditActions.USER_UPDATED, before, saved);
        return saved;
    }

    /**
     * Applies a status transition to an existing user.
     * The handler extracts the domain {@link UserStatus} from the request DTO and passes it here.
     *
     * @param userId    target user identifier
     * @param newStatus the status to transition to
     */
    @Transactional
    public User updateUserStatus(UUID userId, UserStatus newStatus) {
        User user = loadUser(userId);
        User before = copyForAudit(user);
        user.setStatus(newStatus);
        if (UserStatus.LOCKED == newStatus) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
        } else if (UserStatus.ACTIVE == newStatus) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        user.setUpdatedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);
        auditService.recordInfo("USER", userId, AuditActions.USER_STATUS_CHANGED, before, saved);
        return saved;
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> UserStatus.DELETED != u.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Creates a shallow copy of the user for audit before-state recording.
     * This avoids the Hibernate proxy issues with dirty tracking on the same entity.
     */
    private User copyForAudit(User user) {
        return User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .mfaEnabled(user.isMfaEnabled())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
