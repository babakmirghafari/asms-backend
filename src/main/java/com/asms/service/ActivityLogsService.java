package com.asms.service;

import com.asms.domain.AuditLog;
import com.asms.model.ActivityLogDto;
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
 * Activity log service — pre-filtered view of audit_logs for user-facing activity feed (AC-3, AC-13).
 * Backed by audit_logs with action LIKE 'ACTIVITY_%'.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogsService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listActivityLogs(
            UUID organizationId, Integer page, Integer size,
            UUID actorId, String category, OffsetDateTime fromDate, OffsetDateTime toDate) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<AuditLog> results = auditLogRepository.findActivityLogs(
                orgId, actorId, category, fromDate, toDate,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toActivityDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    /**
     * Maps domain AuditLog to contract ActivityLogDto.
     * ActivityLogDto (v2.0.0): id, eventType, category, actorId, actorUsername,
     * targetType, targetId, targetDisplayName, organizationId, ipAddress,
     * outcome, summary, timestamp.
     */
    private ActivityLogDto toActivityDto(AuditLog a) {
        ActivityLogDto dto = new ActivityLogDto();
        dto.setId(a.getId());
        dto.setOrganizationId(a.getOrgId());
        dto.setActorId(a.getActorId());
        dto.setActorUsername(a.getActorUsername());
        dto.setEventType(a.getAction());
        dto.setTargetType(a.getTargetType());
        dto.setTargetId(a.getTargetId());
        dto.setTimestamp(a.getCreatedAt());
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
