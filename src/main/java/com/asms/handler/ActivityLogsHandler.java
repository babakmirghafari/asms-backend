package com.asms.handler;

import com.asms.api.ActivityLogsApiDelegate;
import com.asms.domain.AuditLog;
import com.asms.mapper.AuditLogMapper;
import com.asms.model.ActivityLogDto;
import com.asms.model.PagedResponseDto;
import com.asms.service.ActivityLogsService;
import com.asms.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the ActivityLogs API.
 *
 * <p>Implements {@link ActivityLogsApiDelegate}. Delegates all business logic to
 * {@link ActivityLogsService}. Entity → DTO conversion is done by {@link AuditLogMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogsHandler implements ActivityLogsApiDelegate {

    private final ActivityLogsService activityLogsService;
    private final AuditLogMapper auditLogMapper;

    @Override
    public ResponseEntity<PagedResponseDto> listActivityLogs(
            UUID organizationId,
            Integer page,
            Integer size,
            UUID actorId,
            String category,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) {
        Page<AuditLog> logs = activityLogsService.listActivityLogs(
                organizationId, page, size, actorId, category, fromDate, toDate);
        List<ActivityLogDto> dtos = logs.getContent().stream()
                .map(auditLogMapper::toActivityLogDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, logs));
    }

}
