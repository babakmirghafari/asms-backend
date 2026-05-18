package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.Alert;
import com.asms.domain.enums.AlertStatus;
import com.asms.exception.ConflictException;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AcknowledgeAlertRequestDto;
import com.asms.model.EscalateAlertRequestDto;
import com.asms.model.ResolveAlertRequestDto;
import com.asms.repository.AlertRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Security alerts service (AC-3, AC-12, AC-13).
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.AlertsHandler}.
 *
 * <p>Alert lifecycle: OPEN → ACKNOWLEDGED → RESOLVED or ESCALATED.
 * All transitions are org-scoped and produce audit events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertsService {

    private static final Set<AlertStatus> TERMINAL_STATUSES = Set.of(
            AlertStatus.RESOLVED, AlertStatus.SUPPRESSED);

    private final AlertRepository alertRepository;
    private final AuditService auditService;

    @Transactional
    public Alert acknowledgeAlert(UUID alertId, AcknowledgeAlertRequestDto req) {
        Alert alert = loadAlert(alertId);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(TenantContext.getUserId());
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordInfo("ALERT", alertId, AuditActions.ALERT_ACKNOWLEDGED, null, saved);
        return saved;
    }

    @Transactional
    public Alert resolveAlert(UUID alertId, ResolveAlertRequestDto req) {
        Alert alert = loadAlert(alertId);
        if (TERMINAL_STATUSES.contains(alert.getStatus())) {
            throw new ConflictException("ALERT_ALREADY_TERMINAL",
                    "Alert " + alertId + " is already in terminal status: " + alert.getStatus());
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(TenantContext.getUserId());
        alert.setResolvedAt(OffsetDateTime.now());
        alert.setResolutionNote(req.getNote());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordInfo("ALERT", alertId, AuditActions.ALERT_RESOLVED, null, saved);
        return saved;
    }

    @Transactional
    public Alert escalateAlert(UUID alertId, EscalateAlertRequestDto req) {
        Alert alert = loadAlert(alertId);
        AlertStatus currentStatus = alert.getStatus();
        if (AlertStatus.ESCALATED == currentStatus || TERMINAL_STATUSES.contains(currentStatus)) {
            throw new ConflictException("ALERT_CANNOT_ESCALATE",
                    "Alert " + alertId + " cannot be escalated from status: " + currentStatus);
        }
        alert.setStatus(AlertStatus.ESCALATED);
        alert.setEscalatedBy(TenantContext.getUserId());
        alert.setEscalatedAt(OffsetDateTime.now());
        alert.setEscalationReason(req.getReason());
        alert.setEscalatedTo(req.getEscalateTo());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordWarning("ALERT", alertId, AuditActions.ALERT_ESCALATED, null, saved);
        log.info("[STUB] Escalation notification not yet implemented — target: {}, alert: {}",
                req.getEscalateTo(), alertId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Alert getAlertById(UUID alertId) {
        return loadAlert(alertId);
    }

    @Transactional(readOnly = true)
    public Page<Alert> listAlerts(
            Integer page, Integer size, UUID organizationId, String type, String status) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        AlertStatus statusEnum = status != null ? AlertStatus.valueOf(status) : null;
        return alertRepository.findFiltered(
                orgId, type, statusEnum,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private Alert loadAlert(UUID alertId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return alertRepository.findByOrgIdAndId(orgId, alertId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
        }
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
    }
}
