package com.asms.service;

import com.asms.domain.PermissionGroup;
import com.asms.domain.User;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AddPermissionGroupMembersRequestDto;
import com.asms.model.CreatePermissionGroupRequestDto;
import com.asms.model.UpdatePermissionGroupRequestDto;
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
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public PermissionGroup addPermissionGroupMembers(
            UUID groupId, AddPermissionGroupMembersRequestDto req) {
        PermissionGroup group = loadGroup(groupId);

        for (UUID userId : req.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            group.getMembers().add(user);
        }
        group.setUpdatedAt(OffsetDateTime.now());
        PermissionGroup saved = permissionGroupRepository.save(group);
        invalidatePermissionCache(req.getUserIds(), group.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "GROUP_MEMBERS_ADDED", null, saved);
        return saved;
    }

    @Transactional
    public PermissionGroup createPermissionGroup(CreatePermissionGroupRequestDto req) {
        UUID orgId = TenantContext.getRequiredOrgId();
        PermissionGroup group = PermissionGroup.builder()
                .orgId(orgId)
                .name(req.getName())
                .description(req.getDescription())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

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
        invalidatePermissionCache(affectedUserIds, group.getOrgId());
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
        return permissionGroupRepository.findByOrgId(
                orgId, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @Transactional
    public void removePermissionGroupMember(UUID groupId, UUID userId) {
        PermissionGroup group = loadGroup(groupId);
        group.getMembers().removeIf(u -> u.getId().equals(userId));
        group.setUpdatedAt(OffsetDateTime.now());
        permissionGroupRepository.save(group);
        invalidatePermissionCache(List.of(userId), group.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "GROUP_MEMBER_REMOVED", null, null);
    }

    @Transactional
    public PermissionGroup updatePermissionGroup(UUID groupId, UpdatePermissionGroupRequestDto req) {
        PermissionGroup group = loadGroup(groupId);
        if (req.getName() != null)        group.setName(req.getName());
        if (req.getDescription() != null) group.setDescription(req.getDescription());
        group.setUpdatedAt(OffsetDateTime.now());
        PermissionGroup saved = permissionGroupRepository.save(group);
        List<UUID> affectedUserIds = saved.getMembers().stream().map(User::getId).toList();
        invalidatePermissionCache(affectedUserIds, saved.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId,
                "PERMISSION_GROUP_UPDATED", null, saved);
        return saved;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private PermissionGroup loadGroup(UUID groupId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return permissionGroupRepository.findByOrgIdAndId(orgId, groupId)
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
