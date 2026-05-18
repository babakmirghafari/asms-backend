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
import com.asms.util.PageResponseBuilder;
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
        java.util.List<java.util.UUID> memberIds = addPermissionGroupMembersRequestDto != null
                ? addPermissionGroupMembersRequestDto.getUserIds()
                : java.util.List.of();
        PermissionGroup group = permissionGroupsService.addPermissionGroupMembers(groupId, memberIds);
        return ResponseEntity.ok(permissionGroupMapper.toDto(group));
    }

    @Override
    public ResponseEntity<PermissionGroupDto> createPermissionGroup(
            CreatePermissionGroupRequestDto createPermissionGroupRequestDto) {
        PermissionGroup entity = permissionGroupMapper.toPermissionGroupEntity(
                createPermissionGroupRequestDto);
        PermissionGroup group = permissionGroupsService.createPermissionGroup(entity);
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
        return ResponseEntity.ok(PageResponseBuilder.build(slice, members.size(), pageNum, pageSize));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissionGroups(
            Integer page, Integer size, UUID organizationId) {
        Page<PermissionGroup> groups =
                permissionGroupsService.listPermissionGroups(page, size, organizationId);
        List<PermissionGroupDto> dtos = groups.getContent().stream().map(permissionGroupMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, groups));
    }

    @Override
    public ResponseEntity<Void> removePermissionGroupMember(UUID groupId, UUID userId) {
        permissionGroupsService.removePermissionGroupMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PermissionGroupDto> updatePermissionGroup(
            UUID groupId, UpdatePermissionGroupRequestDto updatePermissionGroupRequestDto) {
        String name = updatePermissionGroupRequestDto != null
                ? updatePermissionGroupRequestDto.getName() : null;
        String description = updatePermissionGroupRequestDto != null
                ? updatePermissionGroupRequestDto.getDescription() : null;
        PermissionGroup updated = permissionGroupsService.updatePermissionGroup(
                groupId, name, description);
        return ResponseEntity.ok(permissionGroupMapper.toDto(updated));
    }

}
