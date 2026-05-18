package com.asms.handler;

import com.asms.api.SessionsApiDelegate;
import com.asms.domain.Session;
import com.asms.mapper.SessionMapper;
import com.asms.model.PagedResponseDto;
import com.asms.model.RevokeAllSessionsRequestDto;
import com.asms.util.PageResponseBuilder;
import com.asms.model.RevokeAllSessionsResponseDto;
import com.asms.model.RevokeSessionRequestDto;
import com.asms.model.SessionDto;
import com.asms.service.SessionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Sessions API.
 *
 * <p>Implements {@link SessionsApiDelegate}. Delegates all business logic to {@link SessionsService}.
 * Entity → DTO conversion is done by {@link SessionMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionsHandler implements SessionsApiDelegate {

    private final SessionsService sessionsService;
    private final SessionMapper sessionMapper;

    @Override
    public ResponseEntity<SessionDto> getSessionById(UUID sessionId) {
        return ResponseEntity.ok(sessionMapper.toDto(sessionsService.getSessionById(sessionId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listSessions(
            Integer page, Integer size, UUID userId, UUID organizationId, String status) {
        Page<Session> sessions = sessionsService.listSessions(page, size, userId, organizationId, status);
        List<SessionDto> dtos = sessions.getContent().stream().map(sessionMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, sessions));
    }

    @Override
    public ResponseEntity<RevokeAllSessionsResponseDto> revokeAllSessions(
            RevokeAllSessionsRequestDto revokeAllSessionsRequestDto) {
        int revoked = sessionsService.revokeAllSessions(revokeAllSessionsRequestDto);
        RevokeAllSessionsResponseDto response = new RevokeAllSessionsResponseDto();
        response.setRevokedCount(revoked);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SessionDto> revokeSession(UUID sessionId,
            RevokeSessionRequestDto revokeSessionRequestDto) {
        Session session = sessionsService.revokeSession(sessionId);
        return ResponseEntity.ok(sessionMapper.toDto(session));
    }

}
