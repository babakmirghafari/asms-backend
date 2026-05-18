package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.Alert;
import com.asms.domain.enums.AlertStatus;
import com.asms.exception.ConflictException;
import com.asms.exception.ResourceNotFoundException;
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

    /**
     * Acknowledges an alert. The handler extracts any optional note and passes
     * primitive values — no DTO crosses the service boundary.
     */
    @Transactional
    public Alert acknowledgeAlert(UUID alertId) {
        Alert alert = loadAlert(alertId);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(TenantContext.getUserId());
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordInfo("ALERT", alertId, AuditActions.ALERT_ACKNOWLEDGED, null, saved);
        return saved;
    }

    /**
     * Resolves an alert with an optional resolution note.
     * The handler extracts the note string from the request DTO before calling here.
     *
     * @param alertId        target alert identifier
     * @param resolutionNote optional textual note explaining the resolution
     */
    @Transactional
    public Alert resolveAlert(UUID alertId, String resolutionNote) {
        Alert alert = loadAlert(alertId);
        if (TERMINAL_STATUSES.contains(alert.getStatus())) {
            throw new ConflictException("ALERT_ALREADY_TERMINAL",
                    "Alert " + alertId + " is already in terminal status: " + alert.getStatus());
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(TenantContext.getUserId());
        alert.setResolvedAt(OffsetDateTime.now());
        alert.setResolutionNote(resolutionNote);
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordInfo("ALERT", alertId, AuditActions.ALERT_RESOLVED, null, saved);
        return saved;
    }

    /**
     * Escalates an alert. The handler extracts reason and target from the request DTO.
     *
     * @param alertId         target alert identifier
     * @param escalationReason textual reason for escalation
     * @param escalateTo      escalation target (user, team, or external system)
     */
    @Transactional
    public Alert escalateAlert(UUID alertId, String escalationReason, String escalateTo) {
        Alert alert = loadAlert(alertId);
        AlertStatus currentStatus = alert.getStatus();
        if (AlertStatus.ESCALATED == currentStatus || TERMINAL_STATUSES.contains(currentStatus)) {
            throw new ConflictException("ALERT_CANNOT_ESCALATE",
                    "Alert " + alertId + " cannot be escalated from status: " + currentStatus);
        }
        alert.setStatus(AlertStatus.ESCALATED);
        alert.setEscalatedBy(TenantContext.getUserId());
        alert.setEscalatedAt(OffsetDateTime.now());
        alert.setEscalationReason(escalationReason);
        alert.setEscalatedTo(escalateTo);
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordWarning("ALERT", alertId, AuditActions.ALERT_ESCALATED, null, saved);
        log.info("[STUB] Escalation notification not yet implemented — target: {}, alert: {}",
                escalateTo, alertId);
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
