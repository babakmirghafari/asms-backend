package com.asms.handler;

import com.asms.api.PermissionGroupsApiDelegate;
import com.asms.model.AddPermissionGroupMembersRequestDto;
import com.asms.model.CreatePermissionGroupRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.PermissionGroupDto;
import com.asms.model.UpdatePermissionGroupRequestDto;
import com.asms.service.PermissionGroupsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the PermissionGroups API.
 *
 * <p>Implements {@link PermissionGroupsApiDelegate}. Delegates all business logic to {@link PermissionGroupsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionGroupsHandler implements PermissionGroupsApiDelegate {

    private final PermissionGroupsService permissionGroupsService;

    @Override
    public ResponseEntity<PermissionGroupDto> addPermissionGroupMembers(
            UUID groupId, AddPermissionGroupMembersRequestDto addPermissionGroupMembersRequestDto) {
        return permissionGroupsService.addPermissionGroupMembers(groupId, addPermissionGroupMembersRequestDto);
    }

    @Override
    public ResponseEntity<PermissionGroupDto> createPermissionGroup(
            CreatePermissionGroupRequestDto createPermissionGroupRequestDto) {
        return permissionGroupsService.createPermissionGroup(createPermissionGroupRequestDto);
    }

    @Override
    public ResponseEntity<Void> deletePermissionGroup(UUID groupId) {
        return permissionGroupsService.deletePermissionGroup(groupId);
    }

    @Override
    public ResponseEntity<PermissionGroupDto> getPermissionGroupById(UUID groupId) {
        return permissionGroupsService.getPermissionGroupById(groupId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissionGroupMembers(
            UUID groupId, Integer page, Integer size) {
        return permissionGroupsService.listPermissionGroupMembers(groupId, page, size);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissionGroups(
            Integer page, Integer size, UUID organizationId) {
        return permissionGroupsService.listPermissionGroups(page, size, organizationId);
    }

    @Override
    public ResponseEntity<Void> removePermissionGroupMember(UUID groupId, UUID userId) {
        return permissionGroupsService.removePermissionGroupMember(groupId, userId);
    }

    @Override
    public ResponseEntity<PermissionGroupDto> updatePermissionGroup(
            UUID groupId, UpdatePermissionGroupRequestDto updatePermissionGroupRequestDto) {
        return permissionGroupsService.updatePermissionGroup(groupId, updatePermissionGroupRequestDto);
    }
}
