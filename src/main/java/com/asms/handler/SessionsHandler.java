package com.asms.handler;

import com.asms.api.SessionsApiDelegate;
import com.asms.model.PagedResponseDto;
import com.asms.model.RevokeAllSessionsRequestDto;
import com.asms.model.RevokeAllSessionsResponseDto;
import com.asms.model.RevokeSessionRequestDto;
import com.asms.model.SessionDto;
import com.asms.service.SessionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Sessions API.
 *
 * <p>Implements {@link SessionsApiDelegate}. Delegates all business logic to {@link SessionsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionsHandler implements SessionsApiDelegate {

    private final SessionsService sessionsService;

    @Override
    public ResponseEntity<SessionDto> getSessionById(UUID sessionId) {
        return sessionsService.getSessionById(sessionId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listSessions(
            Integer page, Integer size, UUID userId, UUID organizationId, String status) {
        return sessionsService.listSessions(page, size, userId, organizationId, status);
    }

    @Override
    public ResponseEntity<RevokeAllSessionsResponseDto> revokeAllSessions(
            RevokeAllSessionsRequestDto revokeAllSessionsRequestDto) {
        return sessionsService.revokeAllSessions(revokeAllSessionsRequestDto);
    }

    @Override
    public ResponseEntity<SessionDto> revokeSession(UUID sessionId, RevokeSessionRequestDto revokeSessionRequestDto) {
        return sessionsService.revokeSession(sessionId, revokeSessionRequestDto);
    }
}
