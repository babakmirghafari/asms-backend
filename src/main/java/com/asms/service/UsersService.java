package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.Membership;
import com.asms.domain.Organization;
import com.asms.domain.Permission;
import com.asms.domain.PermissionGroup;
import com.asms.domain.User;
import com.asms.domain.enums.DeliveryMethod;
import com.asms.domain.enums.Department;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.UserRole;
import com.asms.domain.enums.UserStatus;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateUserRequestDto;
import com.asms.repository.MembershipRepository;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.PermissionRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    /**
     * Creates a new user from the incoming request DTO.
     *
     * <p>Owns the full creation flow:
     * <ol>
     *   <li>Maps base fields from the DTO and sets lifecycle defaults.</li>
     *   <li>Persists the user row.</li>
     *   <li>Creates one {@link Membership} per entry in {@code dto.getOrganizationIds()}
     *       (role = MEMBER, status = ACTIVE). Orgs that cannot be found are skipped with
     *       a warning — partial success is preferred over a full rollback on a bad UUID.</li>
     *   <li>Records an audit event.</li>
     * </ol>
     *
     * @param dto the contract-generated create-user request
     * @return the persisted {@link User}
     */
    @Transactional
    public User createUser(CreateUserRequestDto dto) {
        OffsetDateTime now = OffsetDateTime.now();

        // Resolve delivery method: new deliveryMethod field takes precedence over the legacy
        // sendTempPassword boolean. If neither is set, default to Email.
        DeliveryMethod deliveryMethod;
        if (dto.getDeliveryMethod() != null) {
            deliveryMethod = switch (dto.getDeliveryMethod()) {
                case SMS         -> DeliveryMethod.SMS;
                case BOTH        -> DeliveryMethod.Both;
                case MANUAL_COPY -> DeliveryMethod.Manuel_Copy;
                default          -> DeliveryMethod.Email; // EMAIL
            };
        } else {
            deliveryMethod = Boolean.TRUE.equals(dto.getSendTempPassword())
                    ? DeliveryMethod.Email
                    : DeliveryMethod.Manuel_Copy;
        }

        // Resolve initial status: ACTIVE maps to ACTIVE; INACTIVE maps to INACTIVE;
        // null / unset defaults to PENDING_ACTIVATION (server-side pre-activation state).
        UserStatus initialStatus = UserStatus.PENDING_ACTIVATION;
        if (dto.getInitialStatus() != null) {
            initialStatus = switch (dto.getInitialStatus()) {
                case ACTIVE   -> UserStatus.ACTIVE;
                case INACTIVE -> UserStatus.INACTIVE;
            };
        }

        User user = User.builder()
                .username(dto.getUsername())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .department(dto.getDepartment() != null ? Department.valueOf(dto.getDepartment()) : null)
                .deliveryMethod(deliveryMethod)
                .mfaEnabled(Boolean.TRUE.equals(dto.getMfaEnabled()))
                .forcePasswordChange(dto.getForcePasswordChange() == null || Boolean.TRUE.equals(dto.getForcePasswordChange()))
                .status(initialStatus)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // failedLoginThreshold: the entity does not have a per-user threshold column — it uses
        // failedLoginAttempts (the current counter). Skip silently as documented.
        if (dto.getFailedLoginThreshold() != null) {
            log.debug("createUser: failedLoginThreshold={} received but no per-user threshold column exists — skipping",
                    dto.getFailedLoginThreshold());
        }

        User saved = userRepository.save(user);

        List<UUID> orgIds = dto.getOrganizationIds();
        if (orgIds != null && !orgIds.isEmpty()) {
            for (UUID orgId : orgIds) {
                Organization org = organizationRepository.findById(orgId).orElse(null);
                if (org == null) {
                    log.warn("createUser: organization {} not found — skipping membership for user {}",
                            orgId, saved.getId());
                    continue;
                }
                membershipRepository.save(Membership.builder()
                        .user(saved)
                        .organization(org)
                        .role(UserRole.MEMBER)
                        .status(MembershipStatus.ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }
        }

        List<UUID> directPermIds = dto.getDirectPermissionIds();
        if (directPermIds != null && !directPermIds.isEmpty()) {
            User withPerms = loadUserWithPermissions(saved.getId());
            Set<UUID> orgIdSet = orgIds != null ? new HashSet<>(orgIds) : Set.of();
            for (UUID permId : directPermIds) {
                permissionRepository.findById(permId).ifPresentOrElse(perm -> {
                    UUID permOrgId = perm.getOrganization() != null ? perm.getOrganization().getId() : null;
                    if (!orgIdSet.isEmpty() && (permOrgId == null || !orgIdSet.contains(permOrgId))) {
                        log.warn("createUser: permission {} does not belong to any of the user's orgs — skipping",
                                permId);
                    } else {
                        withPerms.getDirectPermissions().add(perm);
                    }
                }, () -> log.warn("createUser: permission {} not found — skipping", permId));
            }
            userRepository.save(withPerms);
            saved = withPerms;
        }

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
            Integer page, Integer size, String status, String search, UUID organizationId) {
        // AC-13: org scope — use param if provided, otherwise TenantContext
        UUID orgId = organizationId != null ? organizationId : TenantContext.getOrgId();
        Integer statusKey = null;
        if (status != null && !status.isBlank()) {
            try {
                statusKey = UserStatus.valueOf(status).getKey();
            } catch (IllegalArgumentException e) {
                log.warn("listUsers: unknown status filter '{}' — ignoring", status);
            }
        }
        return userRepository.findAllByOrgId(
                orgId, search, statusKey,
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
        if (patch.getFullName() != null)    user.setFullName(patch.getFullName());
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

    // ─── permission & group management ──────────────────────────────────────

    /**
     * Returns the effective permission set for a user — union of all group permissions
     * and any direct permissions. Callers that need the full result (including source
     * maps and conflict detection) should use {@link EffectivePermissionService} directly.
     *
     * @param userId target user identifier
     * @return deduplicated set of effective {@link Permission} objects
     */
    @Transactional(readOnly = true)
    public Set<Permission> getEffectivePermissions(UUID userId) {
        User user = userRepository.findByIdWithGroupsAndPermissions(userId)
                .filter(u -> UserStatus.DELETED != u.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Set<Permission> effective = new HashSet<>();
        for (PermissionGroup group : user.getPermissionGroups()) {
            effective.addAll(group.getPermissions());
        }
        effective.addAll(user.getDirectPermissions());
        return effective;
    }

    /**
     * Adds the user as a member of the specified permission group.
     * The owning side ({@link PermissionGroup#members}) is updated and persisted.
     *
     * @param userId  target user identifier
     * @param groupId permission group to join
     */
    @Transactional
    public void assignGroupToUser(UUID userId, UUID groupId) {
        User user = loadUser(userId);
        PermissionGroup group = permissionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission group not found: " + groupId));
        group.getMembers().add(user);
        group.setUpdatedAt(OffsetDateTime.now());
        permissionGroupRepository.save(group);
        log.info("User {} added to permission group {}", userId, groupId);
    }

    /**
     * Removes the user from the specified permission group's member set.
     *
     * @param userId  target user identifier
     * @param groupId permission group to leave
     */
    @Transactional
    public void removeGroupFromUser(UUID userId, UUID groupId) {
        PermissionGroup group = permissionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission group not found: " + groupId));
        group.getMembers().removeIf(u -> u.getId().equals(userId));
        group.setUpdatedAt(OffsetDateTime.now());
        permissionGroupRepository.save(group);
        log.info("User {} removed from permission group {}", userId, groupId);
    }

    /**
     * Returns the direct (non-group) permissions currently assigned to the user.
     */
    @Transactional(readOnly = true)
    public Set<Permission> getDirectPermissions(UUID userId) {
        return loadUserWithPermissions(userId).getDirectPermissions();
    }

    /**
     * Replaces the full set of direct permission assignments for a user.
     * Permissions not belonging to any of the user's current organizations are rejected.
     *
     * @param userId        target user identifier
     * @param permissionIds full replacement set of permission IDs (empty = revoke all)
     * @return the updated set of direct permissions
     */
    @Transactional
    public Set<Permission> replaceDirectPermissions(UUID userId, List<UUID> permissionIds) {
        User user = loadUserWithPermissions(userId);

        Set<UUID> userOrgIds = user.getMemberships() == null ? Set.of()
                : user.getMemberships().stream()
                        .filter(m -> MembershipStatus.ACTIVE == m.getStatus())
                        .map(m -> m.getOrganization().getId())
                        .collect(Collectors.toSet());

        Set<Permission> newSet = new HashSet<>();
        for (UUID permId : permissionIds) {
            Permission perm = permissionRepository.findById(permId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permId));
            UUID permOrgId = perm.getOrganization() != null ? perm.getOrganization().getId() : null;
            if (!userOrgIds.isEmpty() && (permOrgId == null || !userOrgIds.contains(permOrgId))) {
                throw new com.asms.exception.ValidationException("PERMISSION_ORG_MISMATCH",
                        "Permission " + permId + " does not belong to any of the user's organizations");
            }
            newSet.add(perm);
        }

        user.getDirectPermissions().clear();
        user.getDirectPermissions().addAll(newSet);
        userRepository.save(user);
        log.info("Direct permissions replaced for user {} — {} permission(s) assigned", userId, newSet.size());
        return user.getDirectPermissions();
    }

    /**
     * Grants a direct permission to a user via the {@code user_permissions} join table.
     *
     * @param userId       target user identifier
     * @param permissionId permission to grant directly
     */
    @Transactional
    public void grantDirectPermission(UUID userId, UUID permissionId) {
        User user = loadUserWithPermissions(userId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        user.getDirectPermissions().add(permission);
        userRepository.save(user);
        log.info("Direct permission {} granted to user {}", permissionId, userId);
    }

    /**
     * Revokes a direct permission from a user, removing the row from the
     * {@code user_permissions} join table.
     *
     * @param userId       target user identifier
     * @param permissionId permission to revoke
     */
    @Transactional
    public void revokeDirectPermission(UUID userId, UUID permissionId) {
        User user = loadUserWithPermissions(userId);
        user.getDirectPermissions().removeIf(p -> p.getId().equals(permissionId));
        userRepository.save(user);
        log.info("Direct permission {} revoked from user {}", permissionId, userId);
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> UserStatus.DELETED != u.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private User loadUserWithPermissions(UUID userId) {
        return userRepository.findByIdWithGroupsAndPermissions(userId)
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
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .department(user.getDepartment())
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
