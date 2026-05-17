package com.asms.service;

import com.asms.repository.AlertRepository;
import com.asms.repository.AuditLogRepository;
import com.asms.repository.SessionRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dashboard KPI aggregation service (AC-3, AC-13).
 *
 * <p>Returns a plain record — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.DashboardHandler}.
 *
 * <p>All counts are org-scoped to the requesting user's org.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertRepository alertRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Aggregated dashboard KPIs for one organisation.
     */
    public record DashboardSummary(
            long low, long medium, long high, long critical,
            long activeSessions, long totalUsers, long activeUsers,
            long lockedUsers, long recentActivityCount) {}

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        log.debug("Get dashboard summary — org: {}", orgId);

        long low      = alertRepository.countOpenBySeverity(orgId, "LOW");
        long medium   = alertRepository.countOpenBySeverity(orgId, "MEDIUM");
        long high     = alertRepository.countOpenBySeverity(orgId, "HIGH");
        long critical = alertRepository.countOpenBySeverity(orgId, "CRITICAL");
        long activeSessions    = sessionRepository.countByOrgIdAndStatus(orgId, "ACTIVE");
        long totalUsers        = userRepository.countNonDeleted();
        long activeUsers       = userRepository.countActive();
        long lockedUsers       = userRepository.countLocked();
        long recentActivity    = auditLogRepository.countByOrgIdAndCreatedAtAfter(
                orgId, OffsetDateTime.now().minusHours(24));

        return new DashboardSummary(low, medium, high, critical,
                activeSessions, totalUsers, activeUsers, lockedUsers, recentActivity);
    }
}
