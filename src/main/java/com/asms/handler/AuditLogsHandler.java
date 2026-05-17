package com.asms.handler;

import com.asms.api.AuditLogsApiDelegate;
import com.asms.domain.AuditLog;
import com.asms.mapper.AuditLogMapper;
import com.asms.model.AuditExportRequestDto;
import com.asms.model.AuditExportResponseDto;
import com.asms.model.AuditLogEntryDto;
import com.asms.model.PagedResponseDto;
import com.asms.service.AuditLogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the AuditLogs API.
 *
 * <p>Implements {@link AuditLogsApiDelegate}. Delegates all business logic to {@link AuditLogsService}.
 * Entity → DTO conversion is done by {@link AuditLogMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogsHandler implements AuditLogsApiDelegate {

    private final AuditLogsService auditLogsService;
    private final AuditLogMapper auditLogMapper;

    @Override
    public ResponseEntity<AuditExportResponseDto> getAuditExportStatus(UUID exportId) {
        UUID id = auditLogsService.getAuditExportStatus(exportId);
        AuditExportResponseDto dto = new AuditExportResponseDto();
        dto.setExportId(id);
        dto.setStatus(AuditExportResponseDto.StatusEnum.PENDING);
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<AuditLogEntryDto> getAuditLogEntryById(UUID entryId) {
        AuditLog entry = auditLogsService.getAuditLogEntryById(entryId);
        return ResponseEntity.ok(auditLogMapper.toAuditLogEntryDto(entry));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listAuditLogEntries(
            Integer page, Integer size, UUID organizationId, UUID actorId,
            String action, OffsetDateTime from, OffsetDateTime to) {
        Page<AuditLog> entries = auditLogsService.listAuditLogEntries(
                page, size, organizationId, actorId, action, from, to);
        List<AuditLogEntryDto> dtos = entries.getContent().stream()
                .map(auditLogMapper::toAuditLogEntryDto).toList();
        return ResponseEntity.ok(buildPage(dtos, entries.getTotalElements(),
                entries.getNumber(), entries.getSize()));
    }

    @Override
    public ResponseEntity<AuditExportResponseDto> requestAuditExport(
            AuditExportRequestDto auditExportRequestDto) {
        UUID exportId = auditLogsService.requestAuditExport(auditExportRequestDto);
        AuditExportResponseDto dto = new AuditExportResponseDto();
        dto.setExportId(exportId);
        dto.setStatus(AuditExportResponseDto.StatusEnum.PENDING);
        dto.setExpiresAt(OffsetDateTime.now().plusHours(24));
        return ResponseEntity.status(202).body(dto);
    }

    // ─── private helpers ────────────────────────────────────────────────────

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
