package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.Organization;
import com.asms.domain.Permission;
import com.asms.domain.PermissionImport;
import com.asms.domain.enums.PermissionImportStatus;
import com.asms.domain.enums.PermissionStatus;
import com.asms.exception.AccessDeniedException;
import com.asms.exception.ConflictException;
import com.asms.exception.ResourceNotFoundException;
import com.asms.exception.ValidationException;
import com.asms.model.PermissionImportValidateResponseDtoIssuesInner;
import com.asms.model.PermissionsSimulateRequestDto;
import com.asms.repository.OrganizationRepository;
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
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.PermissionsHandler}.
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
    private final OrganizationRepository organizationRepository;
    private final PermissionImportRepository permissionImportRepository;
    private final EffectivePermissionService effectivePermissionService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    /**
     * Persists a new permission. The handler validates naming and converts the request DTO
     * to a {@link Permission} entity via the mapper before calling here.
     *
     * <p>Naming pattern validation is performed here because it is a domain invariant,
     * not a presentation concern.
     */
    @Transactional
    public Permission createPermission(Permission perm, UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        perm.setOrganization(org);

        log.debug("Create permission: {} in org: {}", perm.getName(), orgId);

        if (!perm.getName().matches(NAMING_PATTERN)) {
            throw new ValidationException("INVALID_PERMISSION_NAME",
                    "Permission name must follow application.module.action format");
        }

        Permission saved = permissionRepository.save(perm);
        auditService.recordInfo("PERMISSION", saved.getId(),
                AuditActions.PERMISSION_CREATED, null, saved);
        return saved;
    }

    @Transactional
    public void deletePermission(UUID permissionId) {
        Permission perm = loadPermission(permissionId);
        if (PermissionStatus.ACTIVE == perm.getStatus()) {
            throw new ConflictException("PERMISSION_ACTIVE",
                    "Cannot delete an ACTIVE permission — deprecate it first");
        }
        permissionRepository.delete(perm);
        auditService.recordInfo("PERMISSION", permissionId,
                AuditActions.PERMISSION_DELETED, perm, null);
    }

    @Transactional(readOnly = true)
    public Permission getPermissionById(UUID permissionId) {
        return loadPermission(permissionId);
    }

    @Transactional(readOnly = true)
    public Page<Permission> listPermissions(
            Integer page, Integer size, UUID organizationId, String status) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        if (status != null) {
            return permissionRepository.findByOrganizationIdAndStatus(orgId, PermissionStatus.valueOf(status),
                    PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        }
        return permissionRepository.findByOrganizationId(orgId,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    // ─── LIFECYCLE TRANSITION ────────────────────────────────────────────────

    /**
     * Applies a lifecycle status transition to a permission.
     * The handler extracts the target {@link PermissionStatus} from the request DTO before calling here.
     *
     * @param permissionId target permission identifier
     * @param targetStatus the status to transition to
     */
    @Transactional
    public Permission updatePermissionStatus(UUID permissionId, PermissionStatus targetStatus) {
        Permission perm = loadPermission(permissionId);
        PermissionStatus currentStatus = perm.getStatus();

        // Validate allowed transitions: DRAFT→ACTIVE, ACTIVE→DEPRECATED
        boolean allowed = (PermissionStatus.DRAFT == currentStatus && PermissionStatus.ACTIVE == targetStatus)
                || (PermissionStatus.ACTIVE == currentStatus && PermissionStatus.DEPRECATED == targetStatus);

        if (!allowed) {
            throw new ValidationException("INVALID_LIFECYCLE_TRANSITION",
                    "Transition from " + currentStatus + " to " + targetStatus + " is not allowed");
        }

        perm.setStatus(targetStatus);
        perm.setUpdatedAt(OffsetDateTime.now());
        Permission saved = permissionRepository.save(perm);
        auditService.recordInfo("PERMISSION", permissionId,
                AuditActions.PERMISSION_STATUS_CHANGED, null, saved);
        return saved;
    }

    // ─── SIMULATE ────────────────────────────────────────────────────────────

    /**
     * Result of a permission simulation — carries decision and explanation.
     */
    public record SimulateResult(boolean granted, String permissionName, UUID userId,
                                  UUID organizationId, List<String> appliedPolicies, String reason) {}

    /**
     * Simulates whether a given permission is granted for a user in an org.
     *
     * <p>Intentional exception to the "no DTO in service" rule: this is a query/simulation
     * operation with no persisted domain entity. The request carries multiple correlated fields
     * (permissionName, userId, organizationId) that form a coherent query value object.
     * Breaking it into primitives would reduce clarity without improving the architecture.
     */
    @Transactional(readOnly = true)
    public SimulateResult simulatePermission(PermissionsSimulateRequestDto req) {
        log.debug("Simulate permission '{}' for user: {} in org: {}",
                req.getPermissionName(), req.getUserId(), req.getOrganizationId());

        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(req.getOrganizationId())) {
            throw new AccessDeniedException("Cannot simulate permissions for a different organization");
        }

        EffectivePermissionResult result = effectivePermissionService.compute(
                req.getUserId(), req.getOrganizationId());

        boolean granted = result.hasPermission(req.getPermissionName());
        List<String> sources = new ArrayList<>();
        String reason;

        if (granted) {
            result.permissionSources().forEach((permId, groups) -> {
                Permission p = result.permissions().stream()
                        .filter(pp -> pp.getId().equals(permId)
                                && pp.getName().equals(req.getPermissionName()))
                        .findFirst().orElse(null);
                if (p != null) sources.addAll(groups);
            });
            reason = "Permission granted via group(s): " + String.join(", ", sources);
        } else {
            reason = "No matching active permission found in this user's effective permission set";
        }

        return new SimulateResult(granted, req.getPermissionName(), req.getUserId(),
                req.getOrganizationId(), sources, reason);
    }

    // ─── TWO-STEP CSV IMPORT ─────────────────────────────────────────────────

    /**
     * Result of the CSV validation step.
     */
    public record ImportValidateResult(PermissionImport importSession,
                                        List<PermissionImportValidateResponseDtoIssuesInner> issues) {}

    /**
     * Result of the CSV commit step.
     */
    public record ImportCommitResult(UUID importId, int committed, int skipped, int failed) {}

    @Transactional
    public ImportValidateResult validatePermissionsImport(MultipartFile file, UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        log.debug("CSV permission import validate — org: {}, file: {}",
                orgId, file != null ? file.getOriginalFilename() : "null");

        List<PermissionImportValidateResponseDtoIssuesInner> issues = new ArrayList<>();
        int totalRows = 0, validRows = 0, errorRows = 0, warnRows = 0;
        String rawCsv = null;

        if (file == null || file.isEmpty()) {
            PermissionImportValidateResponseDtoIssuesInner issue =
                    new PermissionImportValidateResponseDtoIssuesInner();
            issue.setLineNumber(0);
            issue.setMessage("No file uploaded or file is empty");
            issue.setSeverity(PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.ERROR);
            issues.add(issue);
            errorRows = 1;
        } else {
            try {
                rawCsv = new String(file.getBytes(), StandardCharsets.UTF_8);
                try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                     CSVParser parser = CSVFormat.DEFAULT.builder()
                             .setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

                    int rowNum = 1;
                    for (CSVRecord record : parser) {
                        totalRows++;
                        List<String> rowErrors = validateCsvRow(record, orgId, rowNum);
                        if (rowErrors.isEmpty()) {
                            validRows++;
                        } else {
                            boolean hasError = false;
                            for (String msg : rowErrors) {
                                PermissionImportValidateResponseDtoIssuesInner issue =
                                        new PermissionImportValidateResponseDtoIssuesInner();
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
                PermissionImportValidateResponseDtoIssuesInner issue =
                        new PermissionImportValidateResponseDtoIssuesInner();
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
        return new ImportValidateResult(saved, issues);
    }

    /**
     * Commits a previously validated CSV import session.
     * The handler extracts the import ID from the request DTO before calling here.
     *
     * @param importId the import session identifier from the validate step
     */
    @Transactional
    public ImportCommitResult commitPermissionsImport(UUID importId) {
        log.debug("CSV permission import commit — importId: {}", importId);

        PermissionImport importSession = permissionImportRepository.findById(importId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Import session not found: " + importId));

        if (importSession.getStatus() == PermissionImportStatus.BLOCKED) {
            throw new ConflictException("IMPORT_BLOCKED",
                    "Import session is BLOCKED due to validation errors");
        }
        if (importSession.getStatus() != PermissionImportStatus.PENDING_COMMIT) {
            throw new ConflictException("IMPORT_ALREADY_COMMITTED",
                    "Import session is not in PENDING_COMMIT status");
        }
        if (importSession.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ValidationException("IMPORT_EXPIRED",
                    "Import session has expired — re-validate the CSV file");
        }

        int committed = 0, skipped = 0, failed = 0;
        UUID orgId = importSession.getOrganizationId();
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

        List<Integer> errorLineNumbers = new ArrayList<>();
        if (importSession.getIssuesJson() != null) {
            try {
                List<PermissionImportValidateResponseDtoIssuesInner> storedIssues =
                        objectMapper.readValue(importSession.getIssuesJson(),
                                new TypeReference<>() {});
                for (var issue : storedIssues) {
                    if (PermissionImportValidateResponseDtoIssuesInner.SeverityEnum.ERROR
                            .equals(issue.getSeverity())) {
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
                         .setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

                int rowNum = 1;
                for (CSVRecord record : parser) {
                    if (errorLineNumbers.contains(rowNum)) {
                        skipped++;
                    } else {
                        try {
                            String name = record.get("name");
                            String resource = safeGet(record, "resource");
                            String action = record.get("action");
                            String description = record.isMapped("description")
                                    ? record.get("description") : null;

                            Permission perm = Permission.builder()
                                    .organization(organization)
                                    .name(name)
                                    .description(description)
                                    .resource(resource != null ? resource : name.split("\\.")[0])
                                    .action(action)
                                    .status(PermissionStatus.ACTIVE)
                                    .createdAt(OffsetDateTime.now())
                                    .updatedAt(OffsetDateTime.now())
                                    .build();
                            permissionRepository.save(perm);
                            auditService.recordInfo("PERMISSION", perm.getId(),
                                    AuditActions.PERMISSION_CREATED_VIA_IMPORT, null, perm);
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
                throw new ValidationException("IMPORT_PARSE_FAILED",
                        "Failed to re-parse stored CSV content");
            }
        }

        importSession.setStatus(PermissionImportStatus.COMMITTED);
        importSession.setCommittedAt(OffsetDateTime.now());
        importSession.setCommittedCount(committed);
        importSession.setSkippedCount(skipped);
        importSession.setUpdatedAt(OffsetDateTime.now());
        permissionImportRepository.save(importSession);

        return new ImportCommitResult(importId, committed, skipped, failed);
    }

    @Transactional(readOnly = true)
    public PermissionImport getPermissionsImportReport(UUID importId) {
        return permissionImportRepository.findById(importId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Import session not found: " + importId));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Permission loadPermission(UUID permissionId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return permissionRepository.findByOrganizationIdAndId(orgId, permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Permission not found: " + permissionId));
        }
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found: " + permissionId));
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

        if (name != null && !name.isBlank() && permissionRepository.existsByOrganizationIdAndName(orgId, name)) {
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
}
