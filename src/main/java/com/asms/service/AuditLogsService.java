package com.asms.service;

import com.asms.domain.AuditLog;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AuditExportRequestDto;
import com.asms.model.AuditExportResponseDto;
import com.asms.model.AuditLogEntryDto;
import com.asms.model.PagedResponseDto;
import com.asms.repository.AuditLogRepository;
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
import java.util.UUID;

/**
 * Audit log query service (AC-3, AC-12, AC-13).
 * Append-only writes are done via AuditService. This service handles reads and exports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogsService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<AuditExportResponseDto> getAuditExportStatus(UUID exportId) {
        // Export is async and deferred to v2 (ADR-009). Return stub PENDING.
        AuditExportResponseDto dto = new AuditExportResponseDto();
        dto.setExportId(exportId);
        dto.setStatus(AuditExportResponseDto.StatusEnum.PENDING);
        return ResponseEntity.ok(dto);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<AuditLogEntryDto> getAuditLogEntryById(UUID entryId) {
        UUID orgId = TenantContext.getOrgId();
        AuditLog entry;
        if (orgId != null) {
            entry = auditLogRepository.findByOrgIdAndId(orgId, entryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Audit log entry not found: " + entryId));
        } else {
            entry = auditLogRepository.findById(entryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Audit log entry not found: " + entryId));
        }
        return ResponseEntity.ok(toDto(entry));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listAuditLogEntries(
            Integer page, Integer size, UUID organizationId, UUID actorId,
            String action, OffsetDateTime from, OffsetDateTime to) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<AuditLog> results = auditLogRepository.findFiltered(
                orgId, actorId, action, from, to,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    public ResponseEntity<AuditExportResponseDto> requestAuditExport(AuditExportRequestDto req) {
        // Async export deferred to v2 (ADR-009). Return 202 with PENDING status.
        // AuditExportResponseDto v2: exportId, status, downloadUrl, expiresAt
        AuditExportResponseDto dto = new AuditExportResponseDto();
        dto.setExportId(UUID.randomUUID());
        dto.setStatus(AuditExportResponseDto.StatusEnum.PENDING);
        dto.setExpiresAt(OffsetDateTime.now().plusHours(24));
        return ResponseEntity.status(202).body(dto);
    }

    /**
     * Maps domain AuditLog to contract AuditLogEntryDto.
     * AuditLogEntryDto (v2.0.0): id, eventType, actorId, actorUsername, targetType, targetId,
     * organizationId, ipAddress, outcome, details, timestamp, previousHash.
     */
    private AuditLogEntryDto toDto(AuditLog a) {
        AuditLogEntryDto dto = new AuditLogEntryDto();
        dto.setId(a.getId());
        dto.setOrganizationId(a.getOrgId());
        dto.setActorId(a.getActorId());
        dto.setActorUsername(a.getActorUsername());
        dto.setTargetType(a.getTargetType());
        dto.setTargetId(a.getTargetId());
        // action maps to eventType in v2 contract
        dto.setEventType(a.getAction());
        dto.setTimestamp(a.getCreatedAt());
        dto.setPreviousHash(a.getPreviousHash());
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
