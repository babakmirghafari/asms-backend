package com.asms.handler;

import com.asms.api.DashboardApiDelegate;
import com.asms.model.DashboardSummaryDto;
import com.asms.model.DashboardSummaryDtoOpenAlertsBySeverity;
import com.asms.service.DashboardService;
import com.asms.service.DashboardService.DashboardSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST adapter for the Dashboard API.
 *
 * <p>Implements {@link DashboardApiDelegate}. Delegates all business logic to
 * {@link DashboardService}. Maps the {@link DashboardSummary} record to
 * {@link DashboardSummaryDto}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardHandler implements DashboardApiDelegate {

    private final DashboardService dashboardService;

    @Override
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(UUID organizationId) {
        DashboardSummary summary = dashboardService.getDashboardSummary(organizationId);

        DashboardSummaryDtoOpenAlertsBySeverity bySeverity =
                new DashboardSummaryDtoOpenAlertsBySeverity();
        bySeverity.setLow((int) summary.low());
        bySeverity.setMedium((int) summary.medium());
        bySeverity.setHigh((int) summary.high());
        bySeverity.setCritical((int) summary.critical());
        bySeverity.setTotal((int) (summary.low() + summary.medium()
                + summary.high() + summary.critical()));

        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setOpenAlertsBySeverity(bySeverity);
        dto.setActiveSessions((int) summary.activeSessions());
        dto.setTotalUsers((int) summary.totalUsers());
        dto.setActiveUsers((int) summary.activeUsers());
        dto.setLockedUsers((int) summary.lockedUsers());
        dto.setRecentActivityCount((int) summary.recentActivityCount());
        dto.setGeneratedAt(OffsetDateTime.now());

        return ResponseEntity.ok(dto);
    }
}
