package com.asms.handler;

import com.asms.api.AuditLogsApiDelegate;
import com.asms.model.AuditExportRequestDto;
import com.asms.model.AuditExportResponseDto;
import com.asms.model.AuditLogEntryDto;
import com.asms.model.PagedResponseDto;
import com.asms.service.AuditLogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST adapter for the AuditLogs API.
 *
 * <p>Implements {@link AuditLogsApiDelegate}. Delegates all business logic to {@link AuditLogsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogsHandler implements AuditLogsApiDelegate {

    private final AuditLogsService auditLogsService;

    @Override
    public ResponseEntity<AuditExportResponseDto> getAuditExportStatus(UUID exportId) {
        return auditLogsService.getAuditExportStatus(exportId);
    }

    @Override
    public ResponseEntity<AuditLogEntryDto> getAuditLogEntryById(UUID entryId) {
        return auditLogsService.getAuditLogEntryById(entryId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listAuditLogEntries(
            Integer page, Integer size, UUID organizationId, UUID actorId,
            String action, OffsetDateTime from, OffsetDateTime to) {
        return auditLogsService.listAuditLogEntries(page, size, organizationId, actorId, action, from, to);
    }

    @Override
    public ResponseEntity<AuditExportResponseDto> requestAuditExport(
            AuditExportRequestDto auditExportRequestDto) {
        return auditLogsService.requestAuditExport(auditExportRequestDto);
    }
}
