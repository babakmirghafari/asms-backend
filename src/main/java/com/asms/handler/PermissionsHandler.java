package com.asms.handler;

import com.asms.api.PermissionsApiDelegate;
import com.asms.domain.Permission;
import com.asms.domain.PermissionImport;
import com.asms.domain.enums.PermissionImportStatus;
import com.asms.domain.enums.PermissionStatus;
import com.asms.mapper.PermissionMapper;
import com.asms.model.CreatePermissionRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.PermissionDto;
import com.asms.model.PermissionImportCommitRequestDto;
import com.asms.security.TenantContext;
import com.asms.util.PageResponseBuilder;
import com.asms.model.PermissionImportCommitResponseDto;
import com.asms.model.PermissionImportReportDto;
import com.asms.model.PermissionImportValidateResponseDto;
import com.asms.model.PermissionImportValidateResponseDtoIssuesInner;
import com.asms.model.PermissionsSimulateRequestDto;
import com.asms.model.PermissionsSimulateResponseDto;
import com.asms.model.UpdatePermissionStatusRequestDto;
import com.asms.service.PermissionsService;
import com.asms.service.PermissionsService.ImportCommitResult;
import com.asms.service.PermissionsService.ImportValidateResult;
import com.asms.service.PermissionsService.SimulateResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Permissions API.
 *
 * <p>Implements {@link PermissionsApiDelegate}. Delegates all business logic to
 * {@link PermissionsService}. Entity → DTO conversion is done by {@link PermissionMapper}.
 *
 * <p>v2.0.0 operations included: CRUD, lifecycle transitions, two-step CSV import,
 * and permission simulation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionsHandler implements PermissionsApiDelegate {

    private final PermissionsService permissionsService;
    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<PermissionDto> createPermission(
            CreatePermissionRequestDto createPermissionRequestDto) {
        Permission entity = permissionMapper.toPermissionEntity(createPermissionRequestDto);
        UUID orgId = createPermissionRequestDto.getOrganizationId() != null
                ? createPermissionRequestDto.getOrganizationId()
                : TenantContext.getRequiredOrgId();
        Permission permission = permissionsService.createPermission(entity, orgId);
        return ResponseEntity.status(201).body(permissionMapper.toDto(permission));
    }

    @Override
    public ResponseEntity<Void> deletePermission(UUID permissionId) {
        permissionsService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PermissionDto> getPermissionById(UUID permissionId) {
        return ResponseEntity.ok(permissionMapper.toDto(
                permissionsService.getPermissionById(permissionId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listPermissions(
            Integer page, Integer size, UUID organizationId, List<UUID> organizationIds, String status) {
        Page<Permission> permissions =
                permissionsService.listPermissions(page, size, organizationId, organizationIds, status);
        List<PermissionDto> dtos = permissions.getContent().stream()
                .map(permissionMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, permissions));
    }

    @Override
    public ResponseEntity<String> exportPermissions(
            UUID organizationId, List<UUID> organizationIds, String status, String resource) {
        byte[] csv = permissionsService.exportPermissions(organizationId, organizationIds, status);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"permissions.csv\"")
                .body(new String(csv, StandardCharsets.UTF_8));
    }

    @Override
    public ResponseEntity<PermissionDto> updatePermissionStatus(
            UUID permissionId, UpdatePermissionStatusRequestDto updatePermissionStatusRequestDto) {
        PermissionStatus targetStatus = PermissionStatus.valueOf(
                updatePermissionStatusRequestDto.getStatus().getValue());
        Permission updated = permissionsService.updatePermissionStatus(permissionId, targetStatus);
        return ResponseEntity.ok(permissionMapper.toDto(updated));
    }

    @Override
    public ResponseEntity<PermissionsSimulateResponseDto> simulatePermission(
            PermissionsSimulateRequestDto permissionsSimulateRequestDto) {
        SimulateResult result = permissionsService.simulatePermission(permissionsSimulateRequestDto);
        PermissionsSimulateResponseDto response = new PermissionsSimulateResponseDto();
        response.setDecision(result.granted()
                ? PermissionsSimulateResponseDto.DecisionEnum.GRANTED
                : PermissionsSimulateResponseDto.DecisionEnum.DENIED);
        response.setPermissionName(result.permissionName());
        response.setUserId(result.userId());
        response.setOrganizationId(result.organizationId());
        response.setEvaluatedAt(OffsetDateTime.now());
        response.setAppliedPolicies(result.appliedPolicies());
        response.setReason(result.reason());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PermissionImportValidateResponseDto> validatePermissionsImport(
            MultipartFile file, UUID organizationId) {
        ImportValidateResult result =
                permissionsService.validatePermissionsImport(file, organizationId);
        PermissionImport session = result.importSession();

        PermissionImportValidateResponseDto response = new PermissionImportValidateResponseDto();
        response.setImportId(session.getId());
        response.setTotalRows(session.getTotalRows() != null ? session.getTotalRows() : 0);
        response.setValidRows(session.getValidRows() != null ? session.getValidRows() : 0);
        response.setErrorRows(session.getErrorRows() != null ? session.getErrorRows() : 0);
        response.setWarningRows(session.getWarningRows() != null ? session.getWarningRows() : 0);
        response.setStatus(session.getStatus() == PermissionImportStatus.BLOCKED
                ? PermissionImportValidateResponseDto.StatusEnum.BLOCKED
                : PermissionImportValidateResponseDto.StatusEnum.READY);
        response.setIssues(result.issues());
        response.setExpiresAt(session.getExpiresAt());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PermissionImportCommitResponseDto> commitPermissionsImport(
            PermissionImportCommitRequestDto permissionImportCommitRequestDto) {
        UUID importId = permissionImportCommitRequestDto != null
                ? permissionImportCommitRequestDto.getImportId() : null;
        ImportCommitResult result = permissionsService.commitPermissionsImport(importId);
        PermissionImportCommitResponseDto response = new PermissionImportCommitResponseDto();
        response.setImportId(result.importId());
        response.setCommitted(result.committed());
        response.setSkipped(result.skipped());
        response.setFailed(result.failed());
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<PermissionImportReportDto> getPermissionsImportReport(UUID importId) {
        PermissionImport session = permissionsService.getPermissionsImportReport(importId);

        List<PermissionImportValidateResponseDtoIssuesInner> issues = new ArrayList<>();
        if (session.getIssuesJson() != null) {
            try {
                issues = objectMapper.readValue(session.getIssuesJson(),
                        new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Could not deserialise stored issues for report", e);
            }
        }

        PermissionImportReportDto report = new PermissionImportReportDto();
        report.setImportId(importId);
        report.setPhase(session.getStatus() == PermissionImportStatus.COMMITTED
                ? PermissionImportReportDto.PhaseEnum.COMMITTED
                : PermissionImportReportDto.PhaseEnum.VALIDATED);
        report.setTotalRows(session.getTotalRows() != null ? session.getTotalRows() : 0);
        report.setValidRows(session.getValidRows() != null ? session.getValidRows() : 0);
        report.setErrorRows(session.getErrorRows() != null ? session.getErrorRows() : 0);
        report.setWarningRows(session.getWarningRows() != null ? session.getWarningRows() : 0);
        report.setIssues(issues);
        report.setCommittedAt(session.getCommittedAt());
        report.setCommitted(session.getCommittedCount());
        return ResponseEntity.ok(report);
    }

}
