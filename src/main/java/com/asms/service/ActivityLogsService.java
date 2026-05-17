package com.asms.service;

import com.asms.domain.AuditLog;
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
 * Activity log service — pre-filtered view of audit_logs for user-facing activity feed.
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.ActivityLogsHandler}.
 *
 * <p>Backed by audit_logs with action LIKE 'ACTIVITY_%' (AC-3, AC-13).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogsService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> listActivityLogs(
            UUID organizationId, Integer page, Integer size,
            UUID actorId, String category, OffsetDateTime fromDate, OffsetDateTime toDate) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        return auditLogRepository.findAll(
                AuditLogSpecifications.combineAll(
                        AuditLogSpecifications.belongsToOrg(orgId),
                        AuditLogSpecifications.actionStartsWith("ACTIVITY_"),
                        AuditLogSpecifications.actorIdEquals(actorId),
                        AuditLogSpecifications.targetTypeEquals(category),
                        AuditLogSpecifications.createdAtAfter(fromDate),
                        AuditLogSpecifications.createdAtBefore(toDate)),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }
}
