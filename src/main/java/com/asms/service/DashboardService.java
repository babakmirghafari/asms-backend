package com.asms.service;

import com.asms.model.DashboardSummaryDto;
import com.asms.model.DashboardSummaryDtoOpenAlertsBySeverity;
import com.asms.repository.AlertRepository;
import com.asms.repository.AuditLogRepository;
import com.asms.repository.SessionRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dashboard KPI aggregation service (AC-3, AC-13).
 * All counts are org-scoped to the requesting user's org.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertRepository alertRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        log.debug("Get dashboard summary — org: {}", orgId);

        DashboardSummaryDtoOpenAlertsBySeverity bySeverity = new DashboardSummaryDtoOpenAlertsBySeverity();
        long low      = alertRepository.countOpenBySeverity(orgId, "LOW");
        long medium   = alertRepository.countOpenBySeverity(orgId, "MEDIUM");
        long high     = alertRepository.countOpenBySeverity(orgId, "HIGH");
        long critical = alertRepository.countOpenBySeverity(orgId, "CRITICAL");
        bySeverity.setLow((int) low);
        bySeverity.setMedium((int) medium);
        bySeverity.setHigh((int) high);
        bySeverity.setCritical((int) critical);
        bySeverity.setTotal((int) (low + medium + high + critical));

        long activeSessions    = sessionRepository.countByOrgIdAndStatus(orgId, "ACTIVE");
        long totalUsers        = userRepository.countNonDeleted();
        long activeUsers       = userRepository.countActive();
        long lockedUsers       = userRepository.countLocked();
        long recentActivity    = auditLogRepository.countByOrgIdAndCreatedAtAfter(
                orgId, OffsetDateTime.now().minusHours(24));

        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setOpenAlertsBySeverity(bySeverity);
        summary.setActiveSessions((int) activeSessions);
        summary.setTotalUsers((int) totalUsers);
        summary.setActiveUsers((int) activeUsers);
        summary.setLockedUsers((int) lockedUsers);
        summary.setRecentActivityCount((int) recentActivity);
        summary.setGeneratedAt(OffsetDateTime.now());

        return ResponseEntity.ok(summary);
    }
}
