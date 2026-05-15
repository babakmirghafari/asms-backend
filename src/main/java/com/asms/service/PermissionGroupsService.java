package com.asms.service;

import com.asms.domain.PermissionGroup;
import com.asms.domain.User;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AddPermissionGroupMembersRequestDto;
import com.asms.model.CreatePermissionGroupRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.PermissionGroupDto;
import com.asms.model.UpdatePermissionGroupRequestDto;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Permission group management service (AC-3, AC-11, AC-12, AC-13).
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
    public ResponseEntity<PermissionGroupDto> addPermissionGroupMembers(
            UUID groupId, AddPermissionGroupMembersRequestDto req) {
        PermissionGroup group = loadGroup(groupId);
        PermissionGroupDto before = toDto(group);

        for (UUID userId : req.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            group.getMembers().add(user);
        }
        group.setUpdatedAt(OffsetDateTime.now());
        PermissionGroup saved = permissionGroupRepository.save(group);
        invalidatePermissionCache(req.getUserIds(), group.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "GROUP_MEMBERS_ADDED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    @Transactional
    public ResponseEntity<PermissionGroupDto> createPermissionGroup(CreatePermissionGroupRequestDto req) {
        UUID orgId = TenantContext.getRequiredOrgId();
        PermissionGroup group = PermissionGroup.builder()
                .orgId(orgId)
                .name(req.getName())
                .description(req.getDescription())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        PermissionGroup saved = permissionGroupRepository.save(group);
        auditService.recordInfo("PERMISSION_GROUP", saved.getId(), "PERMISSION_GROUP_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deletePermissionGroup(UUID groupId) {
        PermissionGroup group = loadGroup(groupId);
        PermissionGroupDto before = toDto(group);
        List<UUID> affectedUserIds = group.getMembers().stream().map(User::getId).toList();
        permissionGroupRepository.delete(group);
        invalidatePermissionCache(affectedUserIds, group.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "PERMISSION_GROUP_DELETED", before, null);
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PermissionGroupDto> getPermissionGroupById(UUID groupId) {
        return ResponseEntity.ok(toDto(loadGroup(groupId)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listPermissionGroupMembers(
            UUID groupId, Integer page, Integer size) {
        PermissionGroup group = loadGroup(groupId);
        List<Object> members = group.getMembers().stream()
                .map(u -> (Object) u)
                .toList();
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        int from = Math.min(pageNum * pageSize, members.size());
        int to = Math.min(from + pageSize, members.size());
        List<Object> slice = members.subList(from, to);

        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(slice);
        dto.setTotalElements((long) members.size());
        dto.setNumber(pageNum);
        dto.setSize(pageSize);
        dto.setTotalPages((int) Math.ceil((double) members.size() / pageSize));
        return ResponseEntity.ok(dto);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listPermissionGroups(
            Integer page, Integer size, UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<PermissionGroup> results = permissionGroupRepository.findByOrgId(
                orgId, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<Void> removePermissionGroupMember(UUID groupId, UUID userId) {
        PermissionGroup group = loadGroup(groupId);
        PermissionGroupDto before = toDto(group);
        group.getMembers().removeIf(u -> u.getId().equals(userId));
        group.setUpdatedAt(OffsetDateTime.now());
        permissionGroupRepository.save(group);
        invalidatePermissionCache(List.of(userId), group.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "GROUP_MEMBER_REMOVED", before, toDto(group));
        return ResponseEntity.noContent().build();
    }

    @Transactional
    public ResponseEntity<PermissionGroupDto> updatePermissionGroup(
            UUID groupId, UpdatePermissionGroupRequestDto req) {
        PermissionGroup group = loadGroup(groupId);
        PermissionGroupDto before = toDto(group);
        if (req.getName() != null)        group.setName(req.getName());
        if (req.getDescription() != null) group.setDescription(req.getDescription());
        group.setUpdatedAt(OffsetDateTime.now());
        PermissionGroup saved = permissionGroupRepository.save(group);
        List<UUID> affectedUserIds = saved.getMembers().stream().map(User::getId).toList();
        invalidatePermissionCache(affectedUserIds, saved.getOrgId());
        auditService.recordInfo("PERMISSION_GROUP", groupId, "PERMISSION_GROUP_UPDATED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private PermissionGroup loadGroup(UUID groupId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return permissionGroupRepository.findByOrgIdAndId(orgId, groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission group not found: " + groupId));
        }
        return permissionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission group not found: " + groupId));
    }

    private void invalidatePermissionCache(List<UUID> userIds, UUID orgId) {
        // ADR-008: Redis cache invalidation stubbed until Redis is wired
        log.info("[CACHE-STUB] Would invalidate effective-permission cache for {} users in org {}",
                userIds.size(), orgId);
    }

    private PermissionGroupDto toDto(PermissionGroup g) {
        PermissionGroupDto dto = new PermissionGroupDto();
        dto.setId(g.getId());
        dto.setOrganizationId(g.getOrgId());
        dto.setName(g.getName());
        dto.setDescription(g.getDescription());
        dto.setMemberCount(g.getMembers() != null ? g.getMembers().size() : 0);
        dto.setPermissionCount(g.getPermissions() != null ? g.getPermissions().size() : 0);
        dto.setCreatedAt(g.getCreatedAt());
        dto.setUpdatedAt(g.getUpdatedAt());
        return dto;
    }

    private PagedResponseDto buildPage(List<?> items, long total, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(items.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(total);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return dto;
    }
}
