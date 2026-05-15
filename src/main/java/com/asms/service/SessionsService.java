package com.asms.service;

import com.asms.domain.Session;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.PagedResponseDto;
import com.asms.model.RevokeAllSessionsRequestDto;
import com.asms.model.RevokeAllSessionsResponseDto;
import com.asms.model.RevokeSessionRequestDto;
import com.asms.model.SessionDto;
import com.asms.repository.SessionRepository;
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
 * Session management service (AC-3, AC-12, AC-13).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionsService {

    private final SessionRepository sessionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ResponseEntity<SessionDto> getSessionById(UUID sessionId) {
        Session s = loadSession(sessionId);
        return ResponseEntity.ok(toDto(s));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listSessions(
            Integer page, Integer size, UUID userId, UUID organizationId, String status) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<Session> results = sessionRepository.findFiltered(
                orgId, userId, status,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<RevokeAllSessionsResponseDto> revokeAllSessions(
            RevokeAllSessionsRequestDto req) {
        int revoked = sessionRepository.revokeAllForUser(req.getUserId(), OffsetDateTime.now());
        auditService.recordWarning("SESSION", req.getUserId(), "ALL_SESSIONS_REVOKED",
                null, "revoked=" + revoked);
        // RevokeAllSessionsResponseDto v2: revokedCount, reason (no userId)
        RevokeAllSessionsResponseDto response = new RevokeAllSessionsResponseDto();
        response.setRevokedCount(revoked);
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<SessionDto> revokeSession(UUID sessionId, RevokeSessionRequestDto req) {
        Session session = loadSession(sessionId);
        SessionDto before = toDto(session);
        session.setStatus("REVOKED");
        session.setRevokedAt(OffsetDateTime.now());
        Session saved = sessionRepository.save(session);
        auditService.recordWarning("SESSION", sessionId, "SESSION_REVOKED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    private Session loadSession(UUID sessionId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return sessionRepository.findByOrgIdAndId(orgId, sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        }
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }

    private SessionDto toDto(Session s) {
        SessionDto dto = new SessionDto();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setOrganizationId(s.getOrgId());
        dto.setIpAddress(s.getIpAddress());
        dto.setUserAgent(s.getUserAgent());
        dto.setStatus(SessionDto.StatusEnum.fromValue(s.getStatus()));
        // riskScore is short in domain; SessionDto expects Float
        dto.setRiskScore((float) s.getRiskScore());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setExpiresAt(s.getExpiresAt());
        // lastActivityAt: no separate field in domain — use createdAt as approximation
        dto.setLastActivityAt(s.getCreatedAt());
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
