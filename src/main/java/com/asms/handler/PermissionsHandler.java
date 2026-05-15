package com.asms.handler;

import com.asms.api.PermissionsApiDelegate;
import com.asms.model.CreatePermissionRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.PermissionDto;
import com.asms.model.PermissionImportCommitRequestDto;
import com.asms.model.PermissionImportCommitResponseDto;
import com.asms.model.PermissionImportReportDto;
import com.asms.model.PermissionImportValidateResponseDto;
import com.asms.model.PermissionsSimulateRequestDto;
import com.asms.model.PermissionsSimulateResponseDto;
import com.asms.model.UpdatePermissionStatusRequestDto;
import com.asms.service.PermissionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST adapter for the Permissions API.
 *
 * <p>Implements {@link PermissionsApiDelegate}. Delegates all business logic to {@link PermissionsService}.
 *
 * <p>v2.0.0 operations included: CRUD, lifecycle transitions, two-step CSV import,
 * and permission simulation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionsHandler implements PermissionsApiDelegate {

    private final PermissionsService permissionsService;

    @Override
    public ResponseEntity<PermissionDto> createPermission(
            CreatePermissionRequestDto createPermissionRequestDto) {
        return permissionsService.createPermission(createPermissionRequestDto);
    }

    @Override
    public ResponseEntity<Void> deletePermission(UUID permissionId) {
        return permissionsService.deletePermission(permissionId);
    }

    @Override
    public ResponseEntity<PermissionDto> getPermissionById(UUID permissionId) {
        return permissionsService.getPermissionById(permissionId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissions(
            Integer page, Integer size, UUID organizationId, String status) {
        return permissionsService.listPermissions(page, size, organizationId, status);
    }

    @Override
    public ResponseEntity<PermissionDto> updatePermissionStatus(
            UUID permissionId, UpdatePermissionStatusRequestDto updatePermissionStatusRequestDto) {
        return permissionsService.updatePermissionStatus(permissionId, updatePermissionStatusRequestDto);
    }

    @Override
    public ResponseEntity<PermissionsSimulateResponseDto> simulatePermission(
            PermissionsSimulateRequestDto permissionsSimulateRequestDto) {
        return permissionsService.simulatePermission(permissionsSimulateRequestDto);
    }

    @Override
    public ResponseEntity<PermissionImportValidateResponseDto> validatePermissionsImport(
            MultipartFile file, UUID organizationId) {
        return permissionsService.validatePermissionsImport(file, organizationId);
    }

    @Override
    public ResponseEntity<PermissionImportCommitResponseDto> commitPermissionsImport(
            PermissionImportCommitRequestDto permissionImportCommitRequestDto) {
        return permissionsService.commitPermissionsImport(permissionImportCommitRequestDto);
    }

    @Override
    public ResponseEntity<PermissionImportReportDto> getPermissionsImportReport(UUID importId) {
        return permissionsService.getPermissionsImportReport(importId);
    }
}
