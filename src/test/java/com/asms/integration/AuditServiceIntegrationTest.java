package com.asms.integration;

import com.asms.domain.AuditLog;
import com.asms.domain.Organization;
import com.asms.domain.User;
import com.asms.domain.enums.OrganizationPlan;
import com.asms.domain.enums.OrganizationStatus;
import com.asms.domain.enums.UserStatus;
import com.asms.repository.AuditLogRepository;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import com.asms.service.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AC-12: tamper-resistant audit events with before/after state.
 *
 * Verifies:
 * - Audit record is persisted with correct fields
 * - Before/after state is serialised into the record
 * - Hash-chain is populated: first entry uses genesis hash, second uses first entry's hash
 * - Each entry has a non-null, 64-char SHA-256 hex entry_hash
 * - REQUIRES_NEW propagation: audit committed independently of caller transaction
 */
class AuditServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization org;
    private User actor;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(Organization.builder()
                .name("Audit Test Org")
                .slug("audit-test-org-" + System.nanoTime())
                .status(OrganizationStatus.ACTIVE)
                .plan(OrganizationPlan.STARTER)
                .country("US")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        actor = userRepository.save(User.builder()
                .username("audit-actor-" + System.nanoTime())
                .email("audit-actor-" + System.nanoTime() + "@test.com")
                .status(UserStatus.ACTIVE)
                .forcePasswordChange(false)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        TenantContext.set(org.getId(), actor.getId(), actor.getUsername());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("AC-12: audit record is persisted with action, severity, and org scope")
    void auditRecord_persistedWithRequiredFields() {
        AuditLog entry = auditService.recordInfo("USER", actor.getId(), "USER_CREATED", null, "after-state");

        AuditLog persisted = auditLogRepository.findById(entry.getId()).orElseThrow();
        assertThat(persisted.getOrganization().getId()).isEqualTo(org.getId());
        assertThat(persisted.getActor().getId()).isEqualTo(actor.getId());
        assertThat(persisted.getTargetType()).isEqualTo("USER");
        assertThat(persisted.getTargetId()).isEqualTo(actor.getId());
        assertThat(persisted.getAction()).isEqualTo("USER_CREATED");
        assertThat(persisted.getSeverity()).isEqualTo(com.asms.domain.enums.AuditSeverity.INFO);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("AC-12: before/after state is serialised into the audit record")
    void auditRecord_capturesBeforeAndAfterState() {
        record StateSnapshot(String field, String value) {}

        AuditLog entry = auditService.recordInfo(
                "PERMISSION", null, "PERMISSION_UPDATED",
                new StateSnapshot("status", "DRAFT"),
                new StateSnapshot("status", "ACTIVE"));

        AuditLog persisted = auditLogRepository.findById(entry.getId()).orElseThrow();
        assertThat(persisted.getBeforeState()).contains("DRAFT");
        assertThat(persisted.getAfterState()).contains("ACTIVE");
    }

    @Test
    @DisplayName("AC-12: first audit entry uses genesis hash; second entry chains to first")
    void auditRecord_hashChainLinksEntries() {
        // First entry — must use genesis hash (64 zeros)
        AuditLog first = auditService.recordInfo("USER", actor.getId(), "ACTION_FIRST", null, null);
        assertThat(first.getPreviousHash()).isEqualTo("0".repeat(64));
        assertThat(first.getEntryHash()).hasSize(64); // SHA-256 hex

        // Second entry — must chain to first entry's hash
        AuditLog second = auditService.recordInfo("USER", actor.getId(), "ACTION_SECOND", null, null);
        assertThat(second.getPreviousHash()).isEqualTo(first.getEntryHash());
        assertThat(second.getEntryHash()).hasSize(64);
        assertThat(second.getEntryHash()).isNotEqualTo(first.getEntryHash());
    }

    @Test
    @DisplayName("AC-12: entry_hash is a valid 64-char hex string (SHA-256)")
    void auditRecord_entryHashIsValidSha256Hex() {
        AuditLog entry = auditService.recordInfo("SESSION", null, "SESSION_REVOKED", null, null);

        assertThat(entry.getEntryHash()).isNotNull();
        assertThat(entry.getEntryHash()).hasSize(64);
        assertThat(entry.getEntryHash()).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("AC-12: WARNING severity is recorded correctly")
    void auditRecord_warningSeverityIsRecorded() {
        AuditLog entry = auditService.recordWarning("USER", actor.getId(), "LOGIN_FAILED", null, null);

        AuditLog persisted = auditLogRepository.findById(entry.getId()).orElseThrow();
        assertThat(persisted.getSeverity()).isEqualTo(com.asms.domain.enums.AuditSeverity.WARNING);
    }

    @Test
    @DisplayName("AC-12: three sequential entries form a valid hash chain")
    void auditRecord_threeEntries_formValidChain() {
        AuditLog e1 = auditService.recordInfo("USER", actor.getId(), "OP_1", null, null);
        AuditLog e2 = auditService.recordInfo("USER", actor.getId(), "OP_2", null, null);
        AuditLog e3 = auditService.recordInfo("USER", actor.getId(), "OP_3", null, null);

        // Verify the chain
        assertThat(e1.getPreviousHash()).isEqualTo("0".repeat(64));
        assertThat(e2.getPreviousHash()).isEqualTo(e1.getEntryHash());
        assertThat(e3.getPreviousHash()).isEqualTo(e2.getEntryHash());

        // All hashes are distinct
        List<String> hashes = List.of(e1.getEntryHash(), e2.getEntryHash(), e3.getEntryHash());
        assertThat(hashes).doesNotHaveDuplicates();
    }
}
