-- ASMS Consolidated Schema (V1 + V2 + V3 merged)
-- Multi-tenant isolation: all tables include org_id (ADR-006, RISK-002)
-- Timestamps: TIMESTAMPTZ for timezone-aware audit (ADR-009)
-- Enum columns: SMALLINT with integer keys from ConvertableEnum (see domain/converter)
--   UserStatus:          PENDING_ACTIVATION=1, ACTIVE=2, INACTIVE=3, LOCKED=4, TEMP_PASSWORD=5, PENDING_MFA_ENROLLMENT=6, DELETED=7
--   UserRole:            SUPER_ADMIN=1, ADMIN=2, SECURITY_ANALYST=3, MEMBER=4
--   MembershipStatus:    PENDING=1, ACTIVE=2, SUSPENDED=3, REMOVED=4
--   PermissionStatus:    DRAFT=1, ACTIVE=2, DEPRECATED=3
--   SessionStatus:       ACTIVE=1, EXPIRED=2, REVOKED=3
--   AlertSeverity:       LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4
--   AlertStatus:         OPEN=1, ACKNOWLEDGED=2, INVESTIGATING=3, ESCALATED=4, RESOLVED=5, SUPPRESSED=6
--   AuditSeverity:       INFO=1, WARNING=2, CRITICAL=3
--   PermissionImportStatus: PENDING_COMMIT=1, BLOCKED=2, COMMITTED=3, EXPIRED=4

-- ─── ORGANIZATIONS ─────────────────────────────────────────────────────────

CREATE TABLE organizations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    parent_org_id   UUID         REFERENCES organizations(id),
    data_residency  VARCHAR(50),
    logo_url        TEXT,
    primary_color   VARCHAR(7),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_org_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_organizations_parent ON organizations(parent_org_id);
CREATE INDEX idx_organizations_status ON organizations(status);

-- ─── USERS ──────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username                VARCHAR(100) NOT NULL UNIQUE,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    phone_number            VARCHAR(50),
    status                  INTEGER      NOT NULL DEFAULT 1,  -- UserStatus: 1=PENDING_ACTIVATION
    password_hash           TEXT,
    force_password_change   BOOLEAN      NOT NULL DEFAULT TRUE,
    mfa_enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret_encrypted    TEXT,
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- ─── MEMBERSHIPS ────────────────────────────────────────────────────────────

CREATE TABLE memberships (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    org_id      UUID        NOT NULL REFERENCES organizations(id),
    role        INTEGER     NOT NULL DEFAULT 4,  -- UserRole: 4=MEMBER
    status      INTEGER     NOT NULL DEFAULT 2,  -- MembershipStatus: 2=ACTIVE
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, org_id)
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_org ON memberships(org_id);

-- ─── PERMISSIONS ────────────────────────────────────────────────────────────

CREATE TABLE permissions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID         NOT NULL REFERENCES organizations(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    resource    VARCHAR(255) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    status      INTEGER      NOT NULL DEFAULT 1,  -- PermissionStatus: 1=DRAFT
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE INDEX idx_permissions_org ON permissions(org_id);
CREATE INDEX idx_permissions_status ON permissions(org_id, status);
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

-- ─── SESSIONS ───────────────────────────────────────────────────────────────

CREATE TABLE sessions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    org_id      UUID        NOT NULL REFERENCES organizations(id),
    token_hash  TEXT        NOT NULL UNIQUE,
    ip_address  TEXT,
    user_agent  TEXT,
    status      INTEGER     NOT NULL DEFAULT 1,  -- SessionStatus: 1=ACTIVE
    risk_score  SMALLINT    NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_org ON sessions(org_id);
CREATE INDEX idx_sessions_status ON sessions(status);

-- ─── APPLICATIONS ───────────────────────────────────────────────────────────

CREATE TABLE applications (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                      UUID         NOT NULL REFERENCES organizations(id),
    name                        VARCHAR(255) NOT NULL,
    type                        VARCHAR(20)  NOT NULL,
    client_id                   VARCHAR(255),
    client_secret_hash          TEXT,
    redirect_uris               TEXT[],
    status                      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    secret_expires_at           TIMESTAMPTZ,
    integration_health_status   VARCHAR(25)  NOT NULL DEFAULT 'UNKNOWN',
    saml_entity_id              VARCHAR(255),
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name),
    CONSTRAINT chk_app_type CHECK (type IN ('OIDC', 'SAML', 'API_TOKEN')),
    CONSTRAINT chk_app_integration_health
        CHECK (integration_health_status IN ('HEALTHY', 'DEGRADED', 'UNKNOWN', 'NEVER_CONNECTED'))
);

CREATE INDEX idx_applications_org ON applications(org_id);

-- ─── STATION POLICIES ───────────────────────────────────────────────────────

CREATE TABLE station_policies (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL REFERENCES organizations(id),
    user_id         UUID         REFERENCES users(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    allowed_ips     TEXT[],
    allowed_days    SMALLINT[],
    work_hour_start SMALLINT,
    work_hour_end   SMALLINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_station_policy_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_station_policies_user ON station_policies(user_id);
CREATE INDEX idx_station_policies_org ON station_policies(org_id);

-- ─── AUTH POLICIES ──────────────────────────────────────────────────────────

CREATE TABLE auth_policies (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                      UUID        NOT NULL UNIQUE REFERENCES organizations(id),
    max_failed_attempts         INT         NOT NULL DEFAULT 5,
    lockout_duration_minutes    INT         NOT NULL DEFAULT 15,
    require_mfa                 BOOLEAN     NOT NULL DEFAULT FALSE,
    password_min_length         INT         NOT NULL DEFAULT 12,
    password_require_uppercase  BOOLEAN     NOT NULL DEFAULT TRUE,
    password_require_numbers    BOOLEAN     NOT NULL DEFAULT TRUE,
    password_require_symbols    BOOLEAN     NOT NULL DEFAULT TRUE,
    password_history_count      INT         NOT NULL DEFAULT 5,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── AUDIT LOGS ─────────────────────────────────────────────────────────────
-- Append-only per ADR-009; hash chain for tamper detection.

CREATE TABLE audit_logs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL REFERENCES organizations(id),
    actor_id        UUID         REFERENCES users(id),
    actor_username  VARCHAR(100),
    target_type     VARCHAR(100) NOT NULL,
    target_id       UUID,
    action          VARCHAR(100) NOT NULL,
    before_state    JSONB,
    after_state     JSONB,
    severity        INTEGER      NOT NULL DEFAULT 1,  -- AuditSeverity: 1=INFO
    ip_address      TEXT,
    session_id      UUID,
    previous_hash   TEXT,
    entry_hash      TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_org ON audit_logs(org_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(org_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(org_id, action);

-- ─── ALERTS ─────────────────────────────────────────────────────────────────

CREATE TABLE alerts (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID         NOT NULL REFERENCES organizations(id),
    user_id             UUID         REFERENCES users(id),
    type                VARCHAR(50)  NOT NULL,
    severity            INTEGER      NOT NULL DEFAULT 2,  -- AlertSeverity: 2=MEDIUM
    status              INTEGER      NOT NULL DEFAULT 1,  -- AlertStatus: 1=OPEN
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    metadata            JSONB,
    risk_score          NUMERIC(5,2),
    risk_level          VARCHAR(10),
    acknowledged_by     UUID         REFERENCES users(id),
    acknowledged_at     TIMESTAMPTZ,
    resolved_by         UUID         REFERENCES users(id),
    resolved_at         TIMESTAMPTZ,
    resolution_note     TEXT,
    escalated_by        UUID         REFERENCES users(id),
    escalated_at        TIMESTAMPTZ,
    escalation_reason   TEXT,
    escalated_to        VARCHAR(100),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_alert_risk_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') OR risk_level IS NULL),
    CONSTRAINT chk_alert_risk_score
        CHECK (risk_score >= 0 AND risk_score <= 100 OR risk_score IS NULL)
);

CREATE INDEX idx_alerts_org ON alerts(org_id);
CREATE INDEX idx_alerts_status ON alerts(org_id, status);
CREATE INDEX idx_alerts_type ON alerts(org_id, type);

-- ─── PERMISSION IMPORTS ─────────────────────────────────────────────────────
-- Backs the two-step CSV import flow (ARC42 §6 Flow 6).

CREATE TABLE permission_imports (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID        NOT NULL REFERENCES organizations(id),
    status              INTEGER     NOT NULL DEFAULT 1,  -- PermissionImportStatus: 1=PENDING_COMMIT
    total_rows          INT,
    valid_rows          INT,
    error_rows          INT,
    warning_rows        INT,
    raw_csv_content     TEXT,
    issues_json         JSONB,
    expires_at          TIMESTAMPTZ NOT NULL,
    committed_at        TIMESTAMPTZ,
    committed_count     INT,
    skipped_count       INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_permission_imports_org ON permission_imports(organization_id);
CREATE INDEX idx_permission_imports_status ON permission_imports(status, expires_at);
