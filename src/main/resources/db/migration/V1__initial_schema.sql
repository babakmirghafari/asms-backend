-- Create dedicated schema (all tables live in asms, never in public)
CREATE SCHEMA IF NOT EXISTS asms;
SET search_path TO asms;

-- ASMS Consolidated Schema
-- Multi-tenant isolation: all tables include org_id (ADR-006, RISK-002)
-- Timestamps: TIMESTAMPTZ for timezone-aware audit (ADR-009)
-- Enum columns: INTEGER with integer keys from ConvertableEnum (see domain/converter)
--   UserStatus:              PENDING_ACTIVATION=1, ACTIVE=2, INACTIVE=3, LOCKED=4, TEMP_PASSWORD=5, PENDING_MFA_ENROLLMENT=6, DELETED=7
--   UserRole:                SUPER_ADMIN=1, ADMIN=2, SECURITY_ANALYST=3, MEMBER=4
--   MembershipStatus:        PENDING=1, ACTIVE=2, SUSPENDED=3, REMOVED=4
--   PermissionStatus:        DRAFT=1, ACTIVE=2, DEPRECATED=3
--   SessionStatus:           ACTIVE=1, EXPIRED=2, REVOKED=3
--   AlertSeverity:           LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4
--   AlertStatus:             OPEN=1, ACKNOWLEDGED=2, INVESTIGATING=3, ESCALATED=4, RESOLVED=5, SUPPRESSED=6
--   AuditSeverity:           INFO=1, WARNING=2, CRITICAL=3
--   PermissionImportStatus:  PENDING_COMMIT=1, BLOCKED=2, COMMITTED=3, EXPIRED=4
--   ConnectorType:           OIDC=1, SAML=2, API_TOKEN=3
--   ApplicationStatus:       ACTIVE=1, INACTIVE=2, SUSPENDED=3, DELETED=4
--   IntegrationHealthStatus: HEALTHY=1, DEGRADED=2, UNKNOWN=3, NEVER_CONNECTED=4
--   StationPolicyStatus:     ACTIVE=1, INACTIVE=2
--   OrganizationStatus:      ACTIVE=1, SUSPENDED=2, DELETED=3
--   AlertRiskLevel:          LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4
--   IdentityProvider:        OKTA=1, AZURE_AD=2, GOOGLE_WORKSPACE=3, AUTH0=4, ONE_LOGIN=5, PING_IDENTITY=6
--   Department:              Finance=1, IT_Security=2, Operations=3, HR=4, Compliance=5, Engineering=6, Customer_Support=7
--   DeliveryMethod:          Email=1

-- ─── ORGANIZATIONS ──────────────────────────────────────────────────────────

CREATE TABLE organizations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(255),
    description     TEXT,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    parent_org_id   UUID         REFERENCES organizations(id),
    data_residency  VARCHAR(50),
    logo_url        TEXT,
    primary_color   VARCHAR(7),
    status          INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_organizations_parent ON organizations(parent_org_id);
CREATE INDEX idx_organizations_status ON organizations(status);

-- ─── ORGANIZATION SETTINGS ──────────────────────────────────────────────────

CREATE TABLE organization_settings (
    id                                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id                   UUID        NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    require_mfa                       BOOLEAN     NOT NULL DEFAULT FALSE,
    force_mfa_on_sensitive            BOOLEAN     NOT NULL DEFAULT FALSE,
    session_timeout                   INTEGER     NOT NULL DEFAULT 30,
    max_concurrent_sessions           INTEGER     NOT NULL DEFAULT 3,
    allowed_ip_cidrs                  TEXT,
    sso_enabled                       BOOLEAN     NOT NULL DEFAULT FALSE,
    identity_provider                 INTEGER,
    auto_provision                    BOOLEAN     NOT NULL DEFAULT FALSE,
    auto_deprovision                  BOOLEAN     NOT NULL DEFAULT FALSE,
    primary_color                     VARCHAR(7),
    custom_login_url                  TEXT,
    welcome_message                   TEXT,
    data_residency                    VARCHAR(50),
    enforce_data_residency_on_exports BOOLEAN     NOT NULL DEFAULT FALSE,
    long_term_audit_retention         BOOLEAN     NOT NULL DEFAULT FALSE,
    gdpr_data_export_endpoint         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── USERS ──────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username                    VARCHAR(100) NOT NULL UNIQUE,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    full_name                   VARCHAR(255),
    phone_number                VARCHAR(50),
    department                  INTEGER,
    manager                     VARCHAR(255),
    status                      INTEGER      NOT NULL DEFAULT 1,
    password_hash               TEXT,
    temporary_password_hash     TEXT,
    temporary_password_expiry   INTEGER,
    force_password_change       BOOLEAN      NOT NULL DEFAULT TRUE,
    mfa_enabled                 BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret_encrypted        TEXT,
    failed_login_attempts       INT          NOT NULL DEFAULT 0,
    locked_until                TIMESTAMPTZ,
    last_login_at               TIMESTAMPTZ,
    delivery_method             INTEGER      NOT NULL DEFAULT 1,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- ─── MEMBERSHIPS ────────────────────────────────────────────────────────────

CREATE TABLE memberships (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    org_id      UUID        NOT NULL REFERENCES organizations(id),
    role        INTEGER     NOT NULL DEFAULT 4,
    status      INTEGER     NOT NULL DEFAULT 2,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, org_id)
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_org  ON memberships(org_id);

-- ─── PERMISSIONS ────────────────────────────────────────────────────────────

CREATE TABLE permissions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID         NOT NULL REFERENCES organizations(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    resource    VARCHAR(255) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    status      INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE INDEX idx_permissions_org      ON permissions(org_id);
CREATE INDEX idx_permissions_status   ON permissions(org_id, status);
CREATE INDEX idx_permissions_resource ON permissions(org_id, resource);

-- ─── PERMISSION GROUPS ──────────────────────────────────────────────────────

CREATE TABLE permission_groups (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id       UUID         NOT NULL REFERENCES organizations(id),
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    is_sensitive BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE INDEX idx_permission_groups_org ON permission_groups(org_id);

CREATE TABLE permission_group_permissions (
    group_id      UUID        NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    permission_id UUID        NOT NULL REFERENCES permissions(id),
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, permission_id)
);

CREATE TABLE permission_group_members (
    group_id    UUID        NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_pgm_user ON permission_group_members(user_id);

-- ─── USER PERMISSIONS (direct assignments) ──────────────────────────────────

CREATE TABLE user_permissions (
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID        NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, permission_id)
);

CREATE INDEX idx_user_permissions_user       ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_permission ON user_permissions(permission_id);

-- ─── SESSIONS ───────────────────────────────────────────────────────────────

CREATE TABLE sessions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    org_id      UUID        NOT NULL REFERENCES organizations(id),
    token_hash  TEXT        NOT NULL UNIQUE,
    ip_address  TEXT,
    user_agent  TEXT,
    status      INTEGER     NOT NULL DEFAULT 1,
    risk_score  INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_sessions_user   ON sessions(user_id);
CREATE INDEX idx_sessions_org    ON sessions(org_id);
CREATE INDEX idx_sessions_status ON sessions(status);

-- ─── APPLICATIONS ───────────────────────────────────────────────────────────

CREATE TABLE applications (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                    UUID         NOT NULL REFERENCES organizations(id),
    name                      VARCHAR(255) NOT NULL,
    type                      INTEGER      NOT NULL,
    client_id                 VARCHAR(255),
    client_secret_hash        TEXT,
    redirect_uris             TEXT[],
    status                    INTEGER      NOT NULL DEFAULT 1,
    secret_expires_at         TIMESTAMPTZ,
    integration_health_status INTEGER      NOT NULL DEFAULT 3,
    saml_entity_id            VARCHAR(255),
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE INDEX idx_applications_org ON applications(org_id);

-- ─── STATION POLICIES ───────────────────────────────────────────────────────

CREATE TABLE station_policies (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL REFERENCES organizations(id),
    user_id         UUID         REFERENCES users(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          INTEGER      NOT NULL DEFAULT 1,
    allowed_ips     TEXT[],
    allowed_days    INTEGER[],
    work_hour_start INTEGER,
    work_hour_end   INTEGER,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_station_policies_user ON station_policies(user_id);
CREATE INDEX idx_station_policies_org  ON station_policies(org_id);

-- ─── AUTH POLICIES ──────────────────────────────────────────────────────────

CREATE TABLE auth_policies (
    id                         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                     UUID        NOT NULL UNIQUE REFERENCES organizations(id),
    max_failed_attempts        INT         NOT NULL DEFAULT 5,
    lockout_duration_minutes   INT         NOT NULL DEFAULT 15,
    require_mfa                BOOLEAN     NOT NULL DEFAULT FALSE,
    password_min_length        INT         NOT NULL DEFAULT 12,
    password_require_uppercase BOOLEAN     NOT NULL DEFAULT TRUE,
    password_require_numbers   BOOLEAN     NOT NULL DEFAULT TRUE,
    password_require_symbols   BOOLEAN     NOT NULL DEFAULT TRUE,
    password_history_count     INT         NOT NULL DEFAULT 5,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── AUDIT LOGS ─────────────────────────────────────────────────────────────

CREATE TABLE audit_logs (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id         UUID         NOT NULL REFERENCES organizations(id),
    actor_id       UUID         REFERENCES users(id),
    actor_username VARCHAR(100),
    target_type    VARCHAR(100) NOT NULL,
    target_id      UUID,
    action         VARCHAR(100) NOT NULL,
    before_state   JSONB,
    after_state    JSONB,
    severity       INTEGER      NOT NULL DEFAULT 1,
    ip_address     TEXT,
    session_id     UUID,
    previous_hash  TEXT,
    entry_hash     TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_org     ON audit_logs(org_id);
CREATE INDEX idx_audit_logs_actor   ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(org_id, created_at DESC);
CREATE INDEX idx_audit_logs_action  ON audit_logs(org_id, action);

-- ─── ALERTS ─────────────────────────────────────────────────────────────────

CREATE TABLE alerts (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id            UUID         NOT NULL REFERENCES organizations(id),
    user_id           UUID         REFERENCES users(id),
    type              VARCHAR(50)  NOT NULL,
    severity          INTEGER      NOT NULL DEFAULT 2,
    status            INTEGER      NOT NULL DEFAULT 1,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    metadata          JSONB,
    risk_score        NUMERIC(5,2),
    risk_level        INTEGER,
    acknowledged_by   UUID         REFERENCES users(id),
    acknowledged_at   TIMESTAMPTZ,
    resolved_by       UUID         REFERENCES users(id),
    resolved_at       TIMESTAMPTZ,
    resolution_note   TEXT,
    escalated_by      UUID         REFERENCES users(id),
    escalated_at      TIMESTAMPTZ,
    escalation_reason TEXT,
    escalated_to      VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_alert_risk_score CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100))
);

CREATE INDEX idx_alerts_org    ON alerts(org_id);
CREATE INDEX idx_alerts_status ON alerts(org_id, status);
CREATE INDEX idx_alerts_type   ON alerts(org_id, type);

-- ─── PERMISSION IMPORTS ─────────────────────────────────────────────────────

CREATE TABLE permission_imports (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL REFERENCES organizations(id),
    status          INTEGER     NOT NULL DEFAULT 1,
    total_rows      INT,
    valid_rows      INT,
    error_rows      INT,
    warning_rows    INT,
    raw_csv_content TEXT,
    issues_json     JSONB,
    expires_at      TIMESTAMPTZ NOT NULL,
    committed_at    TIMESTAMPTZ,
    committed_count INT,
    skipped_count   INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_permission_imports_org    ON permission_imports(organization_id);
CREATE INDEX idx_permission_imports_status ON permission_imports(status, expires_at);

-- ─── SEED DATA (dev/test only) ───────────────────────────────────────────────
-- Credentials: admin / <any password>, mfa.user / <any password>

INSERT INTO organizations (id, name, slug, display_name, domain, description, status, created_at, updated_at)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Acme Corp', 'acme-corp', 'Acme Corporation', 'acmecorp.com', 'Primary enterprise tenant for Acme Corp', 1, now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'Beta LLC',  'beta-llc',  'Beta LLC',         'betallc.io',   'Secondary tenant for Beta LLC',            1, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, email, full_name, status, password_hash,
                   force_password_change, mfa_enabled, failed_login_attempts,
                   delivery_method, created_at, updated_at)
VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001',
     'admin', 'admin@acmecorp.com', 'Admin User',
     2, NULL, FALSE, FALSE, 0, 1, now(), now()),
    ('bbbbbbbb-0000-0000-0000-000000000002',
     'mfa.user', 'mfa@acmecorp.com', 'MFA User',
     2, NULL, FALSE, TRUE, 0, 1, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO memberships (id, user_id, org_id, role, status, created_at, updated_at)
VALUES
    ('cccccccc-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
     2, 2, now(), now()),
    ('cccccccc-0000-0000-0000-000000000002',
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002',
     4, 2, now(), now()),
    ('cccccccc-0000-0000-0000-000000000003',
     'bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
     4, 2, now(), now())
ON CONFLICT DO NOTHING;
