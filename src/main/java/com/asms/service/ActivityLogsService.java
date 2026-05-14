package com.asms.service;

import com.asms.model.PagedResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Activity log service implementing {@link ActivityLogsApiDelegate}.
 *
 * <p>Serves a user-facing activity feed backed by the same {@code audit_logs} table
 * as {@link AuditLogsService}, but with a {@code type=ACTIVITY} filter pre-applied
 * at this delegate level. This allows regular users to see their own activity history
 * without exposing the full forensic audit log (which is admin-only).
 *
 * <h3>Backing data</h3>
 * <p>Queries {@code audit_logs} with a fixed filter on activity-type events
 * (e.g. USER_LOGIN_SUCCESS, PERMISSION_EVALUATED, SESSION_CREATED). The result
 * is mapped to {@code ActivityLogDto} which exposes a user-friendly subset of fields.
 *
 * <h3>Security</h3>
 * <p>The {@code organizationId} query parameter must match the JWT claims of the
 * requesting user. Admins may query any organization in their tenant; regular users
 * are restricted to their own activity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogsService {

    /**
     * Lists user-facing activity log entries with type=ACTIVITY filter pre-applied.
     *
     * <p>Supports pagination and filtering by actor, category, and date range.
     * All results are org-scoped.
     *
     * @param organizationId required — org context (first param per generated delegate)
     * @param page           zero-based page number
     * @param size           page size (max 100)
     * @param actorId        optional filter by actor
     * @param category       optional filter by activity category
     * @param fromDate       optional filter — events on or after this timestamp
     * @param toDate         optional filter — events on or before this timestamp
     */
    public ResponseEntity<PagedResponseDto> listActivityLogs(
            UUID organizationId,
            Integer page,
            Integer size,
            UUID actorId,
            String category,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) {
        log.debug("List activity logs — org: {}, actor: {}, category: {}, from: {}, to: {}",
                organizationId, actorId, category, fromDate, toDate);
        // TODO: query audit_logs with filter:
        //   WHERE org_id = :organizationId
        //     AND action LIKE 'ACTIVITY_%'   -- or a dedicated type discriminator column
        //     AND (:actorId IS NULL OR actor_id = :actorId)
        //     AND (:category IS NULL OR target_type = :category)
        //     AND (:fromDate IS NULL OR created_at >= :fromDate)
        //     AND (:toDate IS NULL OR created_at <= :toDate)
        //   ORDER BY created_at DESC
        //   OFFSET :page * :size LIMIT :size
        // TODO: map audit_logs rows → ActivityLogDto (user-friendly fields only)
        // TODO: return paged response
        return ResponseEntity.ok(new PagedResponseDto());
    }
}
