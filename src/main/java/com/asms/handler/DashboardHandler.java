package com.asms.handler;

import com.asms.api.DashboardApiDelegate;
import com.asms.model.DashboardSummaryDto;
import com.asms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Dashboard API.
 *
 * <p>Implements {@link DashboardApiDelegate}. Delegates all business logic to {@link DashboardService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardHandler implements DashboardApiDelegate {

    private final DashboardService dashboardService;

    @Override
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(UUID organizationId) {
        return dashboardService.getDashboardSummary(organizationId);
    }
}
