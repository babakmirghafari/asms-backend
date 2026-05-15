package com.asms.service;

import com.asms.domain.Alert;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AcknowledgeAlertRequestDto;
import com.asms.model.AlertDto;
import com.asms.model.EscalateAlertRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.ResolveAlertRequestDto;
import com.asms.repository.AlertRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Security alerts service (AC-3, AC-12, AC-13).
 *
 * <p>Alert lifecycle: OPEN → ACKNOWLEDGED → INVESTIGATING → RESOLVED or ESCALATED.
 * All transitions are org-scoped and produce audit events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertsService {

    private static final Set<String> TERMINAL_STATUSES = Set.of("RESOLVED", "SUPPRESSED");

    private final AlertRepository alertRepository;
    private final AuditService auditService;

    @Transactional
    public ResponseEntity<AlertDto> acknowledgeAlert(UUID alertId, AcknowledgeAlertRequestDto req) {
        Alert alert = loadAlert(alertId);
        AlertDto before = toDto(alert);
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(TenantContext.getUserId());
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordInfo("ALERT", alertId, "ALERT_ACKNOWLEDGED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    @Transactional
    public ResponseEntity<AlertDto> resolveAlert(UUID alertId, ResolveAlertRequestDto req) {
        Alert alert = loadAlert(alertId);
        if (TERMINAL_STATUSES.contains(alert.getStatus())) {
            return ResponseEntity.status(409).build();
        }
        AlertDto before = toDto(alert);
        alert.setStatus("RESOLVED");
        alert.setResolvedBy(TenantContext.getUserId());
        alert.setResolvedAt(OffsetDateTime.now());
        alert.setResolutionNote(req.getNote());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordInfo("ALERT", alertId, "ALERT_RESOLVED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    @Transactional
    public ResponseEntity<AlertDto> escalateAlert(UUID alertId, EscalateAlertRequestDto req) {
        Alert alert = loadAlert(alertId);
        if ("ESCALATED".equals(alert.getStatus()) || TERMINAL_STATUSES.contains(alert.getStatus())) {
            return ResponseEntity.status(409).build();
        }
        AlertDto before = toDto(alert);
        alert.setStatus("ESCALATED");
        alert.setEscalatedBy(TenantContext.getUserId());
        alert.setEscalatedAt(OffsetDateTime.now());
        alert.setEscalationReason(req.getReason());
        alert.setEscalatedTo(req.getEscalateTo());
        alert.setUpdatedAt(OffsetDateTime.now());
        Alert saved = alertRepository.save(alert);
        auditService.recordWarning("ALERT", alertId, "ALERT_ESCALATED", before, toDto(saved));
        log.info("[STUB] Escalation notification not yet implemented — target: {}, alert: {}",
                req.getEscalateTo(), alertId);
        return ResponseEntity.ok(toDto(saved));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<AlertDto> getAlertById(UUID alertId) {
        return ResponseEntity.ok(toDto(loadAlert(alertId)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listAlerts(
            Integer page, Integer size, UUID organizationId, String type, String status) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<Alert> results = alertRepository.findFiltered(
                orgId, type, status,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    private Alert loadAlert(UUID alertId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return alertRepository.findByOrgIdAndId(orgId, alertId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
        }
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
    }

    /**
     * Maps domain Alert to contract AlertDto.
     * Note: AlertDto (v2.0.0) surfaces: id, alertType, severity, status, riskScore, riskLevel,
     * title, description, actorId, actorUsername, organizationId, ipAddress,
     * acknowledgedBy, acknowledgedAt, acknowledgeNote, createdAt.
     * Resolve/escalate fields are domain-only (not exposed in v2 contract).
     */
    private AlertDto toDto(Alert a) {
        AlertDto dto = new AlertDto();
        dto.setId(a.getId());
        dto.setOrganizationId(a.getOrgId());
        dto.setAlertType(a.getType());
        dto.setSeverity(AlertDto.SeverityEnum.fromValue(a.getSeverity()));
        dto.setStatus(AlertDto.StatusEnum.fromValue(a.getStatus()));
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setActorId(a.getUserId());
        dto.setAcknowledgedBy(a.getAcknowledgedBy());
        dto.setAcknowledgedAt(a.getAcknowledgedAt());
        if (a.getRiskScore() != null) {
            dto.setRiskScore(a.getRiskScore().floatValue());
        }
        if (a.getRiskLevel() != null) {
            dto.setRiskLevel(AlertDto.RiskLevelEnum.fromValue(a.getRiskLevel()));
        }
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private PagedResponseDto buildPage(List<?> items, long total, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(items.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(total);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return dto;
    }
}
