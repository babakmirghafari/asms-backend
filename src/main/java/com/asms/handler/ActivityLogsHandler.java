package com.asms.handler;

import com.asms.api.ActivityLogsApiDelegate;
import com.asms.model.PagedResponseDto;
import com.asms.service.ActivityLogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST adapter for the ActivityLogs API.
 *
 * <p>Implements {@link ActivityLogsApiDelegate}. Delegates all business logic to {@link ActivityLogsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogsHandler implements ActivityLogsApiDelegate {

    private final ActivityLogsService activityLogsService;

    @Override
    public ResponseEntity<PagedResponseDto> listActivityLogs(
            UUID organizationId,
            Integer page,
            Integer size,
            UUID actorId,
            String category,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) {
        return activityLogsService.listActivityLogs(organizationId, page, size, actorId, category, fromDate, toDate);
    }
}
