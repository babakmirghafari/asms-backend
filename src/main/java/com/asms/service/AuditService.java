package com.asms.service;

import com.asms.domain.AuditLog;
import com.asms.repository.AuditLogRepository;
import com.asms.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Tamper-resistant audit event writer per ADR-009.
 *
 * <p>Every write operation must call {@link #record} to produce an append-only
 * audit entry with before/after state. Hash-chaining is per-org: each row's
 * {@code entry_hash} is SHA-256 of the row's content concatenated with the
 * {@code entry_hash} of the most recent prior row for that org (genesis = 64 zeros).
 *
 * <p>Uses {@code REQUIRES_NEW} propagation so the audit entry is committed even
 * if the outer transaction rolls back — preserving the forensic record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Records an audit event.
     *
     * @param targetType   the entity type being acted upon (e.g. "USER", "PERMISSION")
     * @param targetId     the entity's UUID
     * @param action       the action code (e.g. "USER_CREATED", "PERMISSION_ACTIVATED")
     * @param beforeState  the entity state before the action — null for CREATE operations
     * @param afterState   the entity state after the action — null for DELETE operations
     * @param severity     INFO | WARNING | CRITICAL
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(String targetType,
                            UUID targetId,
                            String action,
                            Object beforeState,
                            Object afterState,
                            String severity) {
        UUID orgId    = TenantContext.getOrgId();
        if (orgId == null) {
            log.debug("Skipping audit for action: {} — no tenant context (pre-auth request)", action);
            return null;
        }
        UUID actorId  = TenantContext.getUserId();
        String actor  = TenantContext.getUsername();

        // Serialise before/after to JSON
        String beforeJson = toJson(beforeState);
        String afterJson  = toJson(afterState);

        // Get previous hash for chain
        String previousHash = auditLogRepository.findLatestByOrgId(orgId)
                .map(AuditLog::getEntryHash)
                .orElse(GENESIS_HASH);

        OffsetDateTime now = OffsetDateTime.now();

        // Compute entry hash — deterministic ordering matches ADR-009 spec
        String entryHash = sha256(
                String.valueOf(orgId) +
                String.valueOf(actorId) +
                action +
                String.valueOf(beforeJson) +
                String.valueOf(afterJson) +
                previousHash +
                now.toInstant().toEpochMilli()
        );

        AuditLog entry = AuditLog.builder()
                .orgId(orgId)
                .actorId(actorId)
                .actorUsername(actor)
                .targetType(targetType)
                .targetId(targetId)
                .action(action)
                .beforeState(beforeJson)
                .afterState(afterJson)
                .severity(severity)
                .previousHash(previousHash)
                .entryHash(entryHash)
                .createdAt(now)
                .build();

        AuditLog saved = auditLogRepository.save(entry);
        log.debug("Audit recorded — action: {}, target: {} {}, severity: {}", action, targetType, targetId, severity);
        return saved;
    }

    // ─── convenience overloads ───────────────────────────────────────────────

    public AuditLog recordInfo(String targetType, UUID targetId, String action,
                                Object before, Object after) {
        return record(targetType, targetId, action, before, after, "INFO");
    }

    public AuditLog recordWarning(String targetType, UUID targetId, String action,
                                   Object before, Object after) {
        return record(targetType, targetId, action, before, after, "WARNING");
    }

    public AuditLog recordCritical(String targetType, UUID targetId, String action,
                                    Object before, Object after) {
        return record(targetType, targetId, action, before, after, "CRITICAL");
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise audit state to JSON — using toString()", e);
            try {
                // Fallback: wrap in a JSON string
                return objectMapper.writeValueAsString(obj.toString());
            } catch (JsonProcessingException ex) {
                return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
            }
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
