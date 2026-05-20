package com.asms.service;

import com.asms.domain.Organization;
import com.asms.domain.PermissionGroup;
import com.asms.domain.User;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Permission group management service (AC-3, AC-11, AC-12, AC-13).
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.PermissionGroupsHandler}.
 *
 * <p>Group membership changes produce audit events and must invalidate the
 * effective-permission cache (ADR-008). Redis cache invalidation is stubbed
 * with a log warning until Redis is wired.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionGroupsService {

    private final PermissionGroupRepository permissionGroupRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * Adds members to a permission group.
     * The handler extracts the {@code memberIds} list from the request DTO before calling here.
     *
     * @param groupId   target group identifier
     * @param memberIds user IDs to add as members
     */
    @Transactional
    public PermissionGroup addPermissionGroupMembers(UUID groupId, List<UUID> memberIds) {
        PermissionGroup group = loadGroup(groupId);

        for (UUID userId : memberIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            group.getMembers().add(user);
        }
        group.setUpdatedAt(OffsetDateTime.now());
        PermissionGroup saved = permissionGroupRepository.save(group);
        invalidatePermissionCache(memberIds, group.getOrganization().getId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "GROUP_MEMBERS_ADDED", null, saved);
        return saved;
    }

    /**
     * Persists a new permission group. The handler converts the request DTO to a
     * {@link PermissionGroup} entity via the mapper before calling here.
     */
    @Transactional
    public PermissionGroup createPermissionGroup(PermissionGroup group, UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        group.setOrganization(org);
        PermissionGroup saved = permissionGroupRepository.save(group);
        auditService.recordInfo("PERMISSION_GROUP", saved.getId(),
                "PERMISSION_GROUP_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deletePermissionGroup(UUID groupId) {
        PermissionGroup group = loadGroup(groupId);
        List<UUID> affectedUserIds = group.getMembers().stream().map(User::getId).toList();
        permissionGroupRepository.delete(group);
        invalidatePermissionCache(affectedUserIds, group.getOrganization().getId());
        auditService.recordInfo("PERMISSION_GROUP", groupId,
                "PERMISSION_GROUP_DELETED", group, null);
    }

    @Transactional(readOnly = true)
    public PermissionGroup getPermissionGroupById(UUID groupId) {
        return loadGroup(groupId);
    }

    @Transactional(readOnly = true)
    public PermissionGroup getPermissionGroupMembers(UUID groupId) {
        return loadGroup(groupId);
    }

    @Transactional(readOnly = true)
    public Page<PermissionGroup> listPermissionGroups(
            Integer page, Integer size, UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        return permissionGroupRepository.findByOrganizationId(
                orgId, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @Transactional
    public void removePermissionGroupMember(UUID groupId, UUID userId) {
        PermissionGroup group = loadGroup(groupId);
        group.getMembers().removeIf(u -> u.getId().equals(userId));
        group.setUpdatedAt(OffsetDateTime.now());
        permissionGroupRepository.save(group);
        invalidatePermissionCache(List.of(userId), group.getOrganization().getId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "GROUP_MEMBER_REMOVED", null, null);
    }

    /**
     * Applies name/description updates to an existing permission group.
     * The handler extracts the fields from the request DTO before calling here.
     *
     * @param groupId     target group identifier
     * @param name        new name, or {@code null} to leave unchanged
     * @param description new description, or {@code null} to leave unchanged
     */
    @Transactional
    public PermissionGroup updatePermissionGroup(UUID groupId, String name, String description) {
        PermissionGroup group = loadGroup(groupId);
        if (name != null)        group.setName(name);
        if (description != null) group.setDescription(description);
        group.setUpdatedAt(OffsetDateTime.now());
        PermissionGroup saved = permissionGroupRepository.save(group);
        List<UUID> affectedUserIds = saved.getMembers().stream().map(User::getId).toList();
        invalidatePermissionCache(affectedUserIds, saved.getOrganization().getId());
        auditService.recordInfo("PERMISSION_GROUP", groupId,
                "PERMISSION_GROUP_UPDATED", null, saved);
        return saved;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private PermissionGroup loadGroup(UUID groupId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return permissionGroupRepository.findByOrganizationIdAndId(orgId, groupId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Permission group not found: " + groupId));
        }
        return permissionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission group not found: " + groupId));
    }

    private void invalidatePermissionCache(List<UUID> userIds, UUID orgId) {
        log.info("[CACHE-STUB] Would invalidate effective-permission cache for {} users in org {}",
                userIds.size(), orgId);
    }
}
