-- ASMS Initial Schema
-- All tables include org_id for multi-tenant isolation (ADR-006, RISK-002 mitigation)
-- Timestamps use TIMESTAMPTZ for timezone-aware audit records (ADR-009)

-- ─── ORGANIZATIONS ─────────────────────────────────────────────────────────

CREATE TABLE organizations (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(100) NOT NULL UNIQUE,
    display_name        VARCHAR(255),
    parent_org_id       UUID        REFERENCES organizations(id),
    data_residency      VARCHAR(50),
    logo_url            TEXT,
    primary_color       VARCHAR(7),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
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
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING_ACTIVATION',
    password_hash           TEXT,
    force_password_change   BOOLEAN      NOT NULL DEFAULT TRUE,
    mfa_enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret_encrypted    TEXT,
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_status CHECK (status IN (
        'PENDING_ACTIVATION', 'ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'
    ))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- ─── MEMBERSHIPS ────────────────────────────────────────────────────────────

CREATE TABLE memberships (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    role            VARCHAR(50)  NOT NULL DEFAULT 'MEMBER',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, org_id),
    CONSTRAINT chk_membership_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REMOVED'))
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_org ON memberships(org_id);

-- ─── PERMISSIONS ────────────────────────────────────────────────────────────

CREATE TABLE permissions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    application     VARCHAR(100) NOT NULL,
    module          VARCHAR(100) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name),
    CONSTRAINT chk_permission_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DEPRECATED'))
);

CREATE INDEX idx_permissions_org ON permissions(org_id);
CREATE INDEX idx_permissions_status ON permissions(org_id, status);
CREATE INDEX idx_permissions_application ON permissions(org_id, application);

-- ─── PERMISSION GROUPS ──────────────────────────────────────────────────────

CREATE TABLE permission_groups (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    is_sensitive    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE INDEX idx_permission_groups_org ON permission_groups(org_id);

CREATE TABLE permission_group_permissions (
    group_id        UUID        NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    permission_id   UUID        NOT NULL REFERENCES permissions(id),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, permission_id)
);

CREATE TABLE permission_group_members (
    group_id        UUID        NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_pgm_user ON permission_group_members(user_id);

-- ─── SESSIONS ───────────────────────────────────────────────────────────────

CREATE TABLE sessions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    token_hash      TEXT        NOT NULL UNIQUE,
    ip_address      INET,
    user_agent      TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    risk_score      SMALLINT     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    CONSTRAINT chk_session_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_org ON sessions(org_id);
CREATE INDEX idx_sessions_status ON sessions(status);

-- ─── APPLICATIONS ───────────────────────────────────────────────────────────

CREATE TABLE applications (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID        NOT NULL REFERENCES organizations(id),
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    client_id           VARCHAR(255),
    client_secret_hash  TEXT,
    redirect_uris       TEXT[],
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    secret_expires_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (org_id, name),
    CONSTRAINT chk_app_type CHECK (type IN ('OIDC', 'SAML', 'API_TOKEN'))
);

CREATE INDEX idx_applications_org ON applications(org_id);

-- ─── STATION POLICIES ───────────────────────────────────────────────────────

CREATE TABLE station_policies (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    user_id         UUID        NOT NULL REFERENCES users(id),
    allowed_ips     TEXT[],
    allowed_days    SMALLINT[],
    work_hour_start SMALLINT,
    work_hour_end   SMALLINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
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
-- Tamper-resistant: no UPDATE/DELETE allowed on this table (enforced via policy)
-- Each row includes a hash chain for integrity verification (ADR-009)

CREATE TABLE audit_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    actor_id        UUID        REFERENCES users(id),
    actor_username  VARCHAR(100),
    target_type     VARCHAR(100) NOT NULL,
    target_id       UUID,
    action          VARCHAR(100) NOT NULL,
    before_state    JSONB,
    after_state     JSONB,
    severity        VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    ip_address      INET,
    session_id      UUID,
    previous_hash   TEXT,
    entry_hash      TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_audit_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE INDEX idx_audit_logs_org ON audit_logs(org_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(org_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(org_id, action);

-- ─── ALERTS ─────────────────────────────────────────────────────────────────

CREATE TABLE alerts (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID        NOT NULL REFERENCES organizations(id),
    user_id         UUID        REFERENCES users(id),
    type            VARCHAR(50)  NOT NULL,
    severity        VARCHAR(20)  NOT NULL DEFAULT 'WARNING',
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    metadata        JSONB,
    acknowledged_by UUID        REFERENCES users(id),
    acknowledged_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_alert_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'ESCALATED', 'RESOLVED'))
);

CREATE INDEX idx_alerts_org ON alerts(org_id);
CREATE INDEX idx_alerts_status ON alerts(org_id, status);
CREATE INDEX idx_alerts_type ON alerts(org_id, type);
