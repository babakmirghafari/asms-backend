package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.Session;
import com.asms.domain.enums.SessionStatus;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.SessionRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Session management service (AC-3, AC-12, AC-13).
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.SessionsHandler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionsService {

    private final SessionRepository sessionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Session getSessionById(UUID sessionId) {
        return loadSession(sessionId);
    }

    @Transactional(readOnly = true)
    public Page<Session> listSessions(
            Integer page, Integer size, UUID userId, UUID organizationId, String status) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        SessionStatus statusEnum = status != null ? SessionStatus.valueOf(status) : null;
        return sessionRepository.findFiltered(
                orgId, userId, statusEnum,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    /**
     * Revokes all active sessions for a user.
     * The handler extracts the userId from the request DTO before calling here.
     *
     * @param userId the user whose active sessions should be revoked
     * @return number of sessions revoked
     */
    @Transactional
    public int revokeAllSessions(UUID userId) {
        int revoked = sessionRepository.revokeAllForUser(
                userId, OffsetDateTime.now(), SessionStatus.REVOKED, SessionStatus.ACTIVE);
        auditService.recordWarning("SESSION", userId,
                AuditActions.ALL_SESSIONS_REVOKED, null, "revoked=" + revoked);
        return revoked;
    }

    @Transactional
    public Session revokeSession(UUID sessionId) {
        Session session = loadSession(sessionId);
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(OffsetDateTime.now());
        Session saved = sessionRepository.save(session);
        auditService.recordWarning("SESSION", sessionId, AuditActions.SESSION_REVOKED, null, saved);
        return saved;
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private Session loadSession(UUID sessionId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return sessionRepository.findByOrgIdAndId(orgId, sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        }
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }
}
