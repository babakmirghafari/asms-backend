package com.asms.handler;

import com.asms.api.AlertsApiDelegate;
import com.asms.domain.Alert;
import com.asms.mapper.AlertMapper;
import com.asms.model.AcknowledgeAlertRequestDto;
import com.asms.model.AlertDto;
import com.asms.model.EscalateAlertRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.ResolveAlertRequestDto;
import com.asms.service.AlertsService;
import com.asms.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Alerts API.
 *
 * <p>Implements {@link AlertsApiDelegate}. Delegates all business logic to {@link AlertsService}.
 * Entity → DTO conversion is done by {@link AlertMapper}.
 *
 * <p>v2.0.0 operations: acknowledge, resolve, escalate, getById, list.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertsHandler implements AlertsApiDelegate {

    private final AlertsService alertsService;
    private final AlertMapper alertMapper;

    @Override
    public ResponseEntity<AlertDto> acknowledgeAlert(
            UUID alertId, AcknowledgeAlertRequestDto acknowledgeAlertRequestDto) {
        Alert alert = alertsService.acknowledgeAlert(alertId);
        return ResponseEntity.ok(alertMapper.toDto(alert));
    }

    @Override
    public ResponseEntity<AlertDto> resolveAlert(
            UUID alertId, ResolveAlertRequestDto resolveAlertRequestDto) {
        String note = resolveAlertRequestDto != null ? resolveAlertRequestDto.getNote() : null;
        Alert alert = alertsService.resolveAlert(alertId, note);
        return ResponseEntity.ok(alertMapper.toDto(alert));
    }

    @Override
    public ResponseEntity<AlertDto> escalateAlert(
            UUID alertId, EscalateAlertRequestDto escalateAlertRequestDto) {
        String reason = escalateAlertRequestDto != null ? escalateAlertRequestDto.getReason() : null;
        String target = escalateAlertRequestDto != null ? escalateAlertRequestDto.getEscalateTo() : null;
        Alert alert = alertsService.escalateAlert(alertId, reason, target);
        return ResponseEntity.ok(alertMapper.toDto(alert));
    }

    @Override
    public ResponseEntity<AlertDto> getAlertById(UUID alertId) {
        return ResponseEntity.ok(alertMapper.toDto(alertsService.getAlertById(alertId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listAlerts(
            Integer page, Integer size, UUID organizationId, String type, String status) {
        Page<Alert> alerts = alertsService.listAlerts(page, size, organizationId, type, status);
        List<AlertDto> dtos = alerts.getContent().stream().map(alertMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, alerts));
    }

}
