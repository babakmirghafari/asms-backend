package com.asms.service;

import com.asms.domain.AuditLog;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.AuditLogRepository;
import com.asms.repository.AuditLogSpecifications;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit log query service (AC-3, AC-12, AC-13).
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.AuditLogsHandler}.
 *
 * <p>Append-only writes are done via {@link AuditService}. This service handles reads and exports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogsService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AuditLog getAuditLogEntryById(UUID entryId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return auditLogRepository.findByOrgIdAndId(orgId, entryId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Audit log entry not found: " + entryId));
        }
        return auditLogRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Audit log entry not found: " + entryId));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAuditLogEntries(
            Integer page, Integer size, UUID organizationId, UUID actorId,
            String action, OffsetDateTime from, OffsetDateTime to) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        return auditLogRepository.findAll(
                AuditLogSpecifications.combineAll(
                        AuditLogSpecifications.belongsToOrg(orgId),
                        AuditLogSpecifications.actorIdEquals(actorId),
                        AuditLogSpecifications.actionEquals(action),
                        AuditLogSpecifications.createdAtAfter(from),
                        AuditLogSpecifications.createdAtBefore(to)),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    /**
     * Initiates an async export — returns the export ID and initial PENDING status.
     * Async processing deferred to v2 (ADR-009).
     *
     * <p>The handler extracts any relevant filter fields from the request DTO before calling here.
     * No DTO crosses the service boundary.
     */
    public UUID requestAuditExport() {
        // Async export deferred to v2. Return a stub UUID.
        return UUID.randomUUID();
    }

    /**
     * Returns stub PENDING status for an export job.
     * Full async export deferred to v2 (ADR-009).
     */
    public UUID getAuditExportStatus(UUID exportId) {
        return exportId;
    }
}
