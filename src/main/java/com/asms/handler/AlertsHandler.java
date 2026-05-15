package com.asms.handler;

import com.asms.api.AlertsApiDelegate;
import com.asms.model.AcknowledgeAlertRequestDto;
import com.asms.model.AlertDto;
import com.asms.model.EscalateAlertRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.ResolveAlertRequestDto;
import com.asms.service.AlertsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Alerts API.
 *
 * <p>Implements {@link AlertsApiDelegate}. Delegates all business logic to {@link AlertsService}.
 *
 * <p>v2.0.0 operations: acknowledge, resolve, escalate, getById, list.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertsHandler implements AlertsApiDelegate {

    private final AlertsService alertsService;

    @Override
    public ResponseEntity<AlertDto> acknowledgeAlert(
            UUID alertId, AcknowledgeAlertRequestDto acknowledgeAlertRequestDto) {
        return alertsService.acknowledgeAlert(alertId, acknowledgeAlertRequestDto);
    }

    @Override
    public ResponseEntity<AlertDto> resolveAlert(
            UUID alertId, ResolveAlertRequestDto resolveAlertRequestDto) {
        return alertsService.resolveAlert(alertId, resolveAlertRequestDto);
    }

    @Override
    public ResponseEntity<AlertDto> escalateAlert(
            UUID alertId, EscalateAlertRequestDto escalateAlertRequestDto) {
        return alertsService.escalateAlert(alertId, escalateAlertRequestDto);
    }

    @Override
    public ResponseEntity<AlertDto> getAlertById(UUID alertId) {
        return alertsService.getAlertById(alertId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listAlerts(
            Integer page, Integer size, UUID organizationId, String type, String status) {
        return alertsService.listAlerts(page, size, organizationId, type, status);
    }
}
