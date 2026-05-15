package com.asms.service;

import com.asms.domain.Permission;
import com.asms.domain.PermissionImport;
import com.asms.domain.PermissionImportStatus;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreatePermissionRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.PermissionDto;
import com.asms.model.PermissionImportCommitRequestDto;
import com.asms.model.PermissionImportCommitResponseDto;
import com.asms.model.PermissionImportReportDto;
import com.asms.model.PermissionImportValidateResponseDto;
import com.asms.model.PermissionImportValidateResponseDtoIssuesInner;
import com.asms.model.PermissionsSimulateRequestDto;
import com.asms.model.PermissionsSimulateResponseDto;
import com.asms.model.UpdatePermissionStatusRequestDto;
import com.asms.repository.PermissionImportRepository;
import com.asms.repository.PermissionRepository;
import com.asms.security.TenantContext;
import com.asms.service.EffectivePermissionService.EffectivePermissionResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permission catalog management service (AC-3, AC-11, AC-12, AC-13).
 *
 * <p>Handles:
 * <ul>
 *   <li>Permission CRUD with org-scope enforcement (AC-13)</li>
 *   <li>Permission lifecycle: DRAFT → ACTIVE → DEPRECATED (Flow 11)</li>
 *   <li>Two-step CSV import: validate → commit (Flow 6)</li>
 *   <li>Permission simulation using the EffectivePermissionService (AC-11, Flow 8)</li>
 *   <li>Audit events on every write (AC-12)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionsService {

    private static final int IMPORT_TTL_MINUTES = 30;
    private static final String NAMING_PATTERN = "^[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+$";

    private final PermissionRepository permissionRepository;
    private final PermissionImportRepository permissionImportRepository;
    private final EffectivePermissionService effectivePermissionService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Transactional
    public ResponseEntity<PermissionDto> createPermission(CreatePermissionRequestDto req) {
        UUID orgId = TenantContext.getRequiredOrgId();
        log.debug("Create permission: {} in org: {}", req.getName(), orgId);

        // Validate naming convention: application.module.action
        if (!req.getName().matches(NAMING_PATTERN)) {
            return ResponseEntity.badRequest().build();
        }

        Permission perm = Permission.builder()
                .orgId(orgId)
                .name(req.getName())
                .description(req.getDescription())
                .resource(req.getResource() != null ? req.getResource() : req.getName().split("\\.")[0])
                .action(req.getAction() != null ? req.getAction().getValue() : "READ")
                .status("DRAFT")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Permission saved = permissionRepository.save(perm);
        auditService.recordInfo("PERMISSION", saved.getId(), "PERMISSION_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deletePermission(UUID permissionId) {
        Permission perm = loadPermission(permissionId);
        if ("ACTIVE".equals(perm.getStatus())) {
            // Cannot delete ACTIVE permissions — must deprecate first
            return ResponseEntity.status(409).build();
        }
        PermissionDto before = toDto(perm);
        permissionRepository.delete(perm);
        auditService.recordInfo("PERMISSION", permissionId, "PERMISSION_DELETED", before, null);
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PermissionDto> getPermissionById(UUID permissionId) {
        return ResponseEntity.ok(toDto(loadPermission(permissionId)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listPermissions(
            Integer page, Integer size, UUID organizationId, String status) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<Permission> results;
        if (status != null) {
            results = permissionRepository.findByOrgIdAndStatus(orgId, status,
                    PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        } else {
            results = permissionRepository.findByOrgId(orgId,
                    PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        }
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    // ─── LIFECYCLE TRANSITION ────────────────────────────────────────────────

    @Transactional
    public ResponseEntity<PermissionDto> updatePermissionStatus(
            UUID permissionId, UpdatePermissionStatusRequestDto req) {
        Permission perm = loadPermission(permissionId);
        String currentStatus = perm.getStatus();
        String targetStatus = req.getStatus().getValue();

        // Validate allowed transitions: DRAFT→ACTIVE, ACTIVE→DEPRECATED
        boolean allowed = ("DRAFT".equals(currentStatus) && "ACTIVE".equals(targetStatus))
                || ("ACTIVE".equals(currentStatus) && "DEPRECATED".equals(targetStatus));

        if (!allowed) {
            log.warn("Rejected lifecycle transition {} → {} for permission {}", currentStatus, targetStatus, permissionId);
            return ResponseEntity.badRequest().build();
        }

        PermissionDto before = toDto(perm);
        perm.setStatus(targetStatus);
        perm.setUpdatedAt(OffsetDateTime.now());
        Permission saved = permissionRepository.save(perm);
        auditService.recordInfo("PERMISSION", permissionId, "PERMISSION_STATUS_CHANGED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    // ─── SIMULATE ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseEntity<PermissionsSimulateResponseDto> simulatePermission(
            PermissionsSimulateRequestDto req) {
        log.debug("Simulate permission '{}' for user: {} in org: {}",
                req.getPermissionName(), req.getUserId(), req.getOrganizationId());

        // AC-13: org scope check
        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(req.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        EffectivePermissionResult result = effectivePermissionService.compute(
                req.getUserId(), req.getOrganizationId());

        boolean granted = result.hasPermission(req.getPermissionName());

        PermissionsSimulateResponseDto response = new PermissionsSimulateResponseDto();
        response.setDecision(granted
                ? PermissionsSimulateResponseDto.DecisionEnum.GRANTED
                : PermissionsSimulateResponseDto.DecisionEnum.DENIED);
        response.setPermissionName(req.getPermissionName());
        response.setUserId(req.getUserId());
        response.setOrganizationId(req.getOrganizationId());
        response.setEvaluatedAt(OffsetDateTime.now());

        if (granted) {
            // Determine which group(s) grant this permission
            List<String> sources = new ArrayList<>();
            result.permissionSources().forEach((permId, groups) -> {
                Permission p = result.permissions().stream()
                        .filter(pp -> pp.getId().equals(permId) && pp.getName().equals(req.getPermissionName()))
                        .findFirst().orElse(null);
                if (p != null) sources.addAll(groups);
            });
            response.setAppliedPolicies(sources);
            response.setReason("Permission granted via group(s): " + String.join(", ", sources));
        } else {
            response.setReason("No matching active permission found in this user's effective permission set");
        }

        return ResponseEntity.ok(response);
    }

    // ─── TWO-STEP CSV IMPORT ─────────────────────────────────────────────────

    @Transactional
    public ResponseEntity<PermissionImportValidateResponseDto> validatePermissionsImport(
            MultipartFile file, UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        log.debug("CSV permission import validate — org: {}, file: {}",
                orgId, file != null ? file.getOriginalFilename() : "null");

        List<PermissionImportValidateResponseDtoIssuesInner> issues = new ArrayList<>();
        int totalRows = 0, validRows = 0, errorRows = 0, warnRows = 0;
        String rawCsv = null;

        if (file == null || file.isEmpty()) {
            PermissionImportValidateResponseDtoIssuesInner issue = new PermissionImportValidateResponseDtoIssuesInner();
            issue.setLineNumber(0);
            issue.setMessage("No file uploaded or file is empty");
            issue.setSeverity(PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.ERROR);
            issues.add(issue);
        } else {
            try {
                rawCsv = new String(file.getBytes(), StandardCharsets.UTF_8);
                try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                     CSVParser parser = CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .build()
                             .parse(reader)) {

                    int rowNum = 1;
                    for (CSVRecord record : parser) {
                        totalRows++;
                        List<String> rowErrors = validateCsvRow(record, orgId, rowNum);
                        if (rowErrors.isEmpty()) {
                            validRows++;
                        } else {
                            boolean hasError = false;
                            for (String msg : rowErrors) {
                                PermissionImportValidateResponseDtoIssuesInner issue = new PermissionImportValidateResponseDtoIssuesInner();
                                issue.setLineNumber(rowNum);
                                issue.setMessage(msg);
                                boolean isError = msg.startsWith("ERROR:");
                                issue.setSeverity(isError
                                        ? PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.ERROR
                                        : PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.WARNING);
                                issues.add(issue);
                                if (isError) hasError = true;
                            }
                            if (hasError) errorRows++; else warnRows++;
                        }
                        rowNum++;
                    }
                }
            } catch (IOException e) {
                log.error("Failed to parse CSV file", e);
                PermissionImportValidateResponseDtoIssuesInner issue = new PermissionImportValidateResponseDtoIssuesInner();
                issue.setLineNumber(0);
                issue.setMessage("ERROR: Could not parse CSV file: " + e.getMessage());
                issue.setSeverity(PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.ERROR);
                issues.add(issue);
                errorRows = 1;
            }
        }

        PermissionImportStatus status = errorRows > 0
                ? PermissionImportStatus.BLOCKED
                : PermissionImportStatus.PENDING_COMMIT;

        // Serialise issues for storage (using the inner class directly)
        String issuesJson = null;
        try {
            issuesJson = objectMapper.writeValueAsString(issues);
        } catch (Exception e) {
            log.warn("Could not serialise issues to JSON", e);
        }

        OffsetDateTime now = OffsetDateTime.now();
        PermissionImport importSession = PermissionImport.builder()
                .organizationId(orgId)
                .status(status)
                .totalRows(totalRows)
                .validRows(validRows)
                .errorRows(errorRows)
                .warningRows(warnRows)
                .rawCsvContent(rawCsv)
                .issuesJson(issuesJson)
                .expiresAt(now.plusMinutes(IMPORT_TTL_MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();

        PermissionImport saved = permissionImportRepository.save(importSession);

        PermissionImportValidateResponseDto response = new PermissionImportValidateResponseDto();
        response.setImportId(saved.getId());
        response.setTotalRows(totalRows);
        response.setValidRows(validRows);
        response.setErrorRows(errorRows);
        response.setWarningRows(warnRows);
        response.setStatus(errorRows > 0
                ? PermissionImportValidateResponseDto.StatusEnum.BLOCKED
                : PermissionImportValidateResponseDto.StatusEnum.READY);
        response.setIssues(issues);
        response.setExpiresAt(saved.getExpiresAt());
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<PermissionImportCommitResponseDto> commitPermissionsImport(
            PermissionImportCommitRequestDto req) {
        UUID importId = req.getImportId();
        log.debug("CSV permission import commit — importId: {}", importId);

        PermissionImport importSession = permissionImportRepository
                .findByIdAndStatusAndExpiresAtAfter(importId, PermissionImportStatus.PENDING_COMMIT, OffsetDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Import session not found, expired, or already committed: " + importId));

        int committed = 0, skipped = 0, failed = 0;
        UUID orgId = importSession.getOrganizationId();

        // Deserialise stored issues to know which rows have errors
        List<Integer> errorLineNumbers = new ArrayList<>();
        if (importSession.getIssuesJson() != null) {
            try {
                List<PermissionImportValidateResponseDtoIssuesInner> issues = objectMapper.readValue(
                        importSession.getIssuesJson(),
                        new TypeReference<List<PermissionImportValidateResponseDtoIssuesInner>>() {});
                for (PermissionImportValidateResponseDtoIssuesInner issue : issues) {
                    if (PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.ERROR.equals(issue.getSeverity())) {
                        errorLineNumbers.add(issue.getLineNumber());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not deserialise stored issues", e);
            }
        }

        if (importSession.getRawCsvContent() != null) {
            try (Reader reader = new java.io.StringReader(importSession.getRawCsvContent());
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .build()
                         .parse(reader)) {

                int rowNum = 1;
                for (CSVRecord record : parser) {
                    if (errorLineNumbers.contains(rowNum)) {
                        skipped++;
                    } else {
                        try {
                            String name = record.get("name");
                            String resource = safeGet(record, "resource");
                            String action = record.get("action");
                            String description = record.isMapped("description") ? record.get("description") : null;

                            Permission perm = Permission.builder()
                                    .orgId(orgId)
                                    .name(name)
                                    .description(description)
                                    .resource(resource != null ? resource : name.split("\\.")[0])
                                    .action(action)
                                    .status("ACTIVE")
                                    .createdAt(OffsetDateTime.now())
                                    .updatedAt(OffsetDateTime.now())
                                    .build();
                            permissionRepository.save(perm);
                            auditService.recordInfo("PERMISSION", perm.getId(),
                                    "PERMISSION_CREATED_VIA_IMPORT", null, toDto(perm));
                            committed++;
                        } catch (Exception e) {
                            log.warn("Failed to commit CSV row {}: {}", rowNum, e.getMessage());
                            failed++;
                        }
                    }
                    rowNum++;
                }
            } catch (IOException e) {
                log.error("Failed to re-parse stored CSV during commit", e);
                return ResponseEntity.status(500).build();
            }
        }

        importSession.setStatus(PermissionImportStatus.COMMITTED);
        importSession.setCommittedAt(OffsetDateTime.now());
        importSession.setCommittedCount(committed);
        importSession.setSkippedCount(skipped);
        importSession.setUpdatedAt(OffsetDateTime.now());
        permissionImportRepository.save(importSession);

        PermissionImportCommitResponseDto response = new PermissionImportCommitResponseDto();
        response.setImportId(importId);
        response.setCommitted(committed);
        response.setSkipped(skipped);
        response.setFailed(failed);
        return ResponseEntity.status(201).body(response);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PermissionImportReportDto> getPermissionsImportReport(UUID importId) {
        PermissionImport importSession = permissionImportRepository.findById(importId)
                .orElseThrow(() -> new ResourceNotFoundException("Import session not found: " + importId));

        List<PermissionImportValidateResponseDtoIssuesInner> issues = new ArrayList<>();
        if (importSession.getIssuesJson() != null) {
            try {
                issues = objectMapper.readValue(importSession.getIssuesJson(),
                        new TypeReference<List<PermissionImportValidateResponseDtoIssuesInner>>() {});
            } catch (Exception e) {
                log.warn("Could not deserialise stored issues for report", e);
            }
        }

        PermissionImportReportDto report = new PermissionImportReportDto();
        report.setImportId(importId);
        report.setPhase(importSession.getStatus() == PermissionImportStatus.COMMITTED
                ? PermissionImportReportDto.PhaseEnum.COMMITTED
                : PermissionImportReportDto.PhaseEnum.VALIDATED);
        report.setTotalRows(importSession.getTotalRows() != null ? importSession.getTotalRows() : 0);
        report.setValidRows(importSession.getValidRows() != null ? importSession.getValidRows() : 0);
        report.setErrorRows(importSession.getErrorRows() != null ? importSession.getErrorRows() : 0);
        report.setWarningRows(importSession.getWarningRows() != null ? importSession.getWarningRows() : 0);
        report.setIssues(issues);
        report.setCommittedAt(importSession.getCommittedAt());
        report.setCommitted(importSession.getCommittedCount());
        return ResponseEntity.ok(report);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Permission loadPermission(UUID permissionId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return permissionRepository.findByOrgIdAndId(orgId, permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        }
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
    }

    private List<String> validateCsvRow(CSVRecord record, UUID orgId, int rowNum) {
        List<String> errors = new ArrayList<>();
        String name = safeGet(record, "name");
        String action = safeGet(record, "action");

        if (name == null || name.isBlank())
            errors.add("ERROR: Row " + rowNum + ": 'name' is required");
        else if (!name.matches(NAMING_PATTERN))
            errors.add("ERROR: Row " + rowNum + ": 'name' must follow application.module.action format");

        if (action == null || action.isBlank())
            errors.add("ERROR: Row " + rowNum + ": 'action' is required");

        // Check for duplicate name within org
        if (name != null && !name.isBlank() && permissionRepository.existsByOrgIdAndName(orgId, name)) {
            errors.add("WARNING: Row " + rowNum + ": permission '" + name + "' already exists in org");
        }

        return errors;
    }

    private String safeGet(CSVRecord record, String column) {
        try {
            return record.isMapped(column) ? record.get(column) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private PermissionDto toDto(Permission p) {
        PermissionDto dto = new PermissionDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setResource(p.getResource());
        dto.setAction(PermissionDto.ActionEnum.fromValue(p.getAction()));
        dto.setStatus(PermissionDto.StatusEnum.fromValue(p.getStatus()));
        dto.setOrganizationId(p.getOrgId());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
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
