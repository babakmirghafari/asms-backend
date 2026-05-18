package com.asms.handler;

import com.asms.api.PermissionGroupsApiDelegate;
import com.asms.domain.PermissionGroup;
import com.asms.mapper.PermissionGroupMapper;
import com.asms.model.AddPermissionGroupMembersRequestDto;
import com.asms.model.CreatePermissionGroupRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.PermissionGroupDto;
import com.asms.model.UpdatePermissionGroupRequestDto;
import com.asms.service.PermissionGroupsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the PermissionGroups API.
 *
 * <p>Implements {@link PermissionGroupsApiDelegate}. Delegates all business logic to
 * {@link PermissionGroupsService}. Maps domain {@link PermissionGroup} ↔
 * {@link PermissionGroupDto} via {@link PermissionGroupMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionGroupsHandler implements PermissionGroupsApiDelegate {

    private final PermissionGroupsService permissionGroupsService;
    private final PermissionGroupMapper permissionGroupMapper;

    @Override
    public ResponseEntity<PermissionGroupDto> addPermissionGroupMembers(
            UUID groupId, AddPermissionGroupMembersRequestDto addPermissionGroupMembersRequestDto) {
        PermissionGroup group = permissionGroupsService.addPermissionGroupMembers(
                groupId, addPermissionGroupMembersRequestDto);
        return ResponseEntity.ok(permissionGroupMapper.toDto(group));
    }

    @Override
    public ResponseEntity<PermissionGroupDto> createPermissionGroup(
            CreatePermissionGroupRequestDto createPermissionGroupRequestDto) {
        PermissionGroup group = permissionGroupsService.createPermissionGroup(
                createPermissionGroupRequestDto);
        return ResponseEntity.status(201).body(permissionGroupMapper.toDto(group));
    }

    @Override
    public ResponseEntity<Void> deletePermissionGroup(UUID groupId) {
        permissionGroupsService.deletePermissionGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PermissionGroupDto> getPermissionGroupById(UUID groupId) {
        return ResponseEntity.ok(permissionGroupMapper.toDto(permissionGroupsService.getPermissionGroupById(groupId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissionGroupMembers(
            UUID groupId, Integer page, Integer size) {
        PermissionGroup group = permissionGroupsService.getPermissionGroupMembers(groupId);
        List<Object> members = group.getMembers() != null
                ? group.getMembers().stream().map(u -> (Object) u.getId()).toList()
                : List.of();
        int pageNum  = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        int from = Math.min(pageNum * pageSize, members.size());
        int to   = Math.min(from + pageSize, members.size());
        List<Object> slice = members.subList(from, to);

        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(slice);
        dto.setTotalElements((long) members.size());
        dto.setNumber(pageNum);
        dto.setSize(pageSize);
        dto.setTotalPages((int) Math.ceil((double) members.size() / pageSize));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissionGroups(
            Integer page, Integer size, UUID organizationId) {
        Page<PermissionGroup> groups =
                permissionGroupsService.listPermissionGroups(page, size, organizationId);
        List<PermissionGroupDto> dtos = groups.getContent().stream().map(permissionGroupMapper::toDto).toList();
        return ResponseEntity.ok(buildPage(dtos, groups.getTotalElements(),
                groups.getNumber(), groups.getSize()));
    }

    @Override
    public ResponseEntity<Void> removePermissionGroupMember(UUID groupId, UUID userId) {
        permissionGroupsService.removePermissionGroupMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PermissionGroupDto> updatePermissionGroup(
            UUID groupId, UpdatePermissionGroupRequestDto updatePermissionGroupRequestDto) {
        PermissionGroup updated = permissionGroupsService.updatePermissionGroup(
                groupId, updatePermissionGroupRequestDto);
        return ResponseEntity.ok(permissionGroupMapper.toDto(updated));
    }

    // ─── private helpers ────────────────────────────────────────────────────

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
