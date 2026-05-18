package com.asms.bdd.steps;

import com.asms.domain.enums.AlertSeverity;
import com.asms.domain.enums.AlertStatus;
import com.asms.domain.enums.AuditSeverity;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.PermissionStatus;
import com.asms.domain.enums.SessionStatus;
import com.asms.domain.enums.UserRole;
import com.asms.domain.enums.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JdbcTemplate-based helper for seeding test data in BDD scenarios.
 * Inserts rows directly into the test PostgreSQL container.
 * Enum columns store integer keys from ConvertableEnum.getKey().
 */
@Component
public class TestDataHelper {

    @Autowired
    private JdbcTemplate jdbc;

    public UUID createOrg() {
        UUID id = UUID.randomUUID();
        String slug = "test-org-" + id.toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO organizations(id, name, slug, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', now(), now())
                """, id, "Test Org " + id.toString().substring(0, 8), slug, "Test Org");
        return id;
    }

    public UUID createUser(UUID orgId, String username, String email,
                           String status, boolean mfaEnabled, boolean forcePasswordChange) {
        UUID id = UUID.randomUUID();
        int statusKey = UserStatus.valueOf(status).getKey();
        jdbc.update("""
                INSERT INTO users(id, username, email, first_name, last_name, status,
                    mfa_enabled, force_password_change, failed_login_attempts, created_at, updated_at)
                VALUES (?, ?, ?, 'Test', 'User', ?, ?, ?, 0, now(), now())
                """, id, username, email, statusKey, mfaEnabled, forcePasswordChange);
        if (orgId != null) {
            jdbc.update("""
                    INSERT INTO memberships(id, user_id, org_id, role, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, now(), now())
                    """, UUID.randomUUID(), id, orgId,
                    UserRole.MEMBER.getKey(), MembershipStatus.ACTIVE.getKey());
        }
        return id;
    }

    public void setUserFailedLoginsAndLock(UUID userId, int count) {
        jdbc.update("""
                UPDATE users
                SET failed_login_attempts = ?,
                    locked_until = now() + interval '30 minutes'
                WHERE id = ?
                """, count, userId);
    }

    public void setUserForcePasswordChange(UUID userId) {
        jdbc.update("UPDATE users SET force_password_change = true WHERE id = ?", userId);
    }

    public UUID createSession(UUID userId, UUID orgId, String status) {
        UUID id = UUID.randomUUID();
        int statusKey = SessionStatus.valueOf(status).getKey();
        jdbc.update("""
                INSERT INTO sessions(id, user_id, org_id, token_hash, status, risk_score,
                    created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, 5, now(), now() + interval '1 hour')
                """, id, userId, orgId, "tok-" + id, statusKey);
        return id;
    }

    public UUID createAlert(UUID orgId, UUID userId, String type, String severity, String status) {
        UUID id = UUID.randomUUID();
        int severityKey = AlertSeverity.valueOf(severity).getKey();
        int statusKey = AlertStatus.valueOf(status).getKey();
        jdbc.update("""
                INSERT INTO alerts(id, org_id, user_id, type, severity, status, title, description,
                    risk_score, risk_level, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'BDD Test Alert', 'Created by BDD test', 50, 'MEDIUM', now(), now())
                """, id, orgId, userId, type, severityKey, statusKey);
        return id;
    }

    public UUID createPermission(UUID orgId, String name, String resource, String action, String status) {
        UUID id = UUID.randomUUID();
        int statusKey = PermissionStatus.valueOf(status).getKey();
        jdbc.update("""
                INSERT INTO permissions(id, org_id, name, description, resource, action, status,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())
                """, id, orgId, name, "BDD test permission", resource, action, statusKey);
        return id;
    }

    public UUID createPermissionGroup(UUID orgId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO permission_groups(id, org_id, name, is_sensitive, created_at, updated_at)
                VALUES (?, ?, ?, false, now(), now())
                """, id, orgId, name);
        return id;
    }

    public void assignPermissionToGroup(UUID groupId, UUID permissionId) {
        jdbc.update("""
                INSERT INTO permission_group_permissions(group_id, permission_id, assigned_at)
                VALUES (?, ?, now())
                """, groupId, permissionId);
    }

    public void assignUserToGroup(UUID groupId, UUID userId) {
        jdbc.update("""
                INSERT INTO permission_group_members(group_id, user_id, assigned_at)
                VALUES (?, ?, now())
                """, groupId, userId);
    }

    /** Creates a PENDING_COMMIT import session with valid CSV content. */
    public UUID createPermissionImportReady(UUID orgId) {
        UUID id = UUID.randomUUID();
        String csv = "name,resource,action,description\nbdd.test.read,bdd,READ,BDD test permission";
        jdbc.update("""
                INSERT INTO permission_imports(id, organization_id, status, total_rows, valid_rows,
                    error_rows, warning_rows, raw_csv_content, issues_json, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, 1, 1, 0, 0, ?, CAST('[]' AS jsonb),
                    now() + interval '30 minutes', now(), now())
                """, id, orgId, 1 /* PENDING_COMMIT */, csv);
        return id;
    }

    /** Creates a BLOCKED import session with error issues. */
    public UUID createPermissionImportBlocked(UUID orgId) {
        UUID id = UUID.randomUUID();
        String csv = "name,resource,action\nbad row,missing,";
        String issues = "[{\"lineNumber\":1,\"severity\":\"ERROR\",\"message\":\"ERROR: bad row\"}]";
        jdbc.update("""
                INSERT INTO permission_imports(id, organization_id, status, total_rows, valid_rows,
                    error_rows, warning_rows, raw_csv_content, issues_json, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, 1, 0, 1, 0, ?, CAST(? AS jsonb),
                    now() + interval '30 minutes', now(), now())
                """, id, orgId, 2 /* BLOCKED */, csv, issues);
        return id;
    }

    /** Creates a COMMITTED import session. */
    public UUID createPermissionImportCommitted(UUID orgId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO permission_imports(id, organization_id, status, total_rows, valid_rows,
                    error_rows, warning_rows, issues_json, committed_count, skipped_count,
                    expires_at, committed_at, created_at, updated_at)
                VALUES (?, ?, ?, 2, 2, 0, 0, CAST('[]' AS jsonb), 2, 0,
                    now() + interval '30 minutes', now(), now(), now())
                """, id, orgId, 3 /* COMMITTED */);
        return id;
    }

    /** Creates a PENDING_COMMIT import session that has already expired. */
    public UUID createPermissionImportExpired(UUID orgId) {
        UUID id = UUID.randomUUID();
        String csv = "name,resource,action\nbdd.exp.read,bdd,READ";
        jdbc.update("""
                INSERT INTO permission_imports(id, organization_id, status, total_rows, valid_rows,
                    error_rows, warning_rows, raw_csv_content, issues_json, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, 1, 1, 0, 0, ?, CAST('[]' AS jsonb),
                    now() - interval '1 hour', now(), now())
                """, id, orgId, 1 /* PENDING_COMMIT */, csv);
        return id;
    }

    /** Upserts a user by username — idempotent, safe to call in scenarios that may run twice. */
    public UUID upsertUser(String username, String email, String status,
                           boolean mfaEnabled, boolean forcePasswordChange) {
        UUID id = UUID.randomUUID();
        int statusKey = UserStatus.valueOf(status).getKey();
        jdbc.update("""
                INSERT INTO users(id, username, email, first_name, last_name, status,
                    mfa_enabled, force_password_change, failed_login_attempts, created_at, updated_at)
                VALUES (?, ?, ?, 'Test', 'User', ?, ?, ?, 0, now(), now())
                ON CONFLICT (username) DO UPDATE SET
                    status = EXCLUDED.status,
                    mfa_enabled = EXCLUDED.mfa_enabled,
                    force_password_change = false,
                    failed_login_attempts = 0,
                    locked_until = null,
                    updated_at = now()
                """, id, username, email, statusKey, mfaEnabled, forcePasswordChange);
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }

    public UUID createAuditActivityLog(UUID orgId, UUID actorId, String actorUsername) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO audit_logs(id, org_id, actor_id, actor_username, target_type,
                    action, severity, entry_hash, created_at)
                VALUES (?, ?, ?, ?, 'USER', 'ACTIVITY_LOGIN_SUCCESS', ?, ?, now())
                """, id, orgId, actorId, actorUsername,
                AuditSeverity.INFO.getKey(), "hash-" + id);
        return id;
    }
}
