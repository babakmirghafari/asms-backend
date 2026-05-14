-- V2 Contract Schema Migration — ASMS API Contract v2.0.0
-- Adds: permission_imports table, Alert new columns, Application new column
-- Note: users.locked_until, sessions.risk_score, permissions.status are already
--       present in V1__initial_schema.sql and require no changes here.

-- ─── PERMISSION IMPORTS ─────────────────────────────────────────────────────
-- Backs the two-step CSV import flow (ARC42 §6 Flow 6):
--   POST /permissions/import/validate → creates a session (PENDING_COMMIT or BLOCKED)
--   POST /permissions/import/commit   → commits the session (COMMITTED)

CREATE TABLE permission_imports (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID            NOT NULL REFERENCES organizations(id),
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING_COMMIT',
    total_rows          INT,
    valid_rows          INT,
    error_rows          INT,
    warning_rows        INT,
    raw_csv_content     TEXT,
    issues_json         JSONB,
    expires_at          TIMESTAMPTZ     NOT NULL,
    committed_at        TIMESTAMPTZ,
    committed_count     INT,
    skipped_count       INT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_permission_import_status CHECK (
        status IN ('PENDING_COMMIT', 'BLOCKED', 'COMMITTED', 'EXPIRED')
    )
);

CREATE INDEX idx_permission_imports_org ON permission_imports(organization_id);
CREATE INDEX idx_permission_imports_status ON permission_imports(status, expires_at);

-- ─── ALERTS — new columns ───────────────────────────────────────────────────
-- riskScore (0-100) and riskLevel (LOW/MEDIUM/HIGH/CRITICAL) added in v2.0.0
-- per §6 Flow 9 (risk scoring engine). nullable: alerts created before v2 have no score.

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS risk_score    NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS risk_level    VARCHAR(10);

-- Extend the severity check to include LOW/MEDIUM/HIGH/CRITICAL (v1 had INFO/WARNING/CRITICAL)
-- Note: existing constraint is dropped and re-created to add LOW and MEDIUM values
ALTER TABLE alerts DROP CONSTRAINT IF EXISTS chk_alert_severity;
ALTER TABLE alerts
    ADD CONSTRAINT chk_alert_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL', 'INFO', 'WARNING'));

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS chk_alert_status;
ALTER TABLE alerts
    ADD CONSTRAINT chk_alert_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'ESCALATED', 'RESOLVED', 'SUPPRESSED'));

ALTER TABLE alerts
    ADD CONSTRAINT chk_alert_risk_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') OR risk_level IS NULL);

ALTER TABLE alerts
    ADD CONSTRAINT chk_alert_risk_score
        CHECK (risk_score >= 0 AND risk_score <= 100 OR risk_score IS NULL);

-- Add resolve/escalate tracking columns to alerts
ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS resolved_by     UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS resolved_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS resolution_note TEXT,
    ADD COLUMN IF NOT EXISTS escalated_by    UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS escalated_at    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS escalation_reason TEXT,
    ADD COLUMN IF NOT EXISTS escalated_to    VARCHAR(100);

-- ─── APPLICATIONS — new column ──────────────────────────────────────────────
-- integrationHealthStatus added in v2.0.0 per §6 Flow 7 (application health monitoring).
-- Default UNKNOWN for all existing rows.

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS integration_health_status VARCHAR(25) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE applications
    ADD CONSTRAINT chk_app_integration_health
        CHECK (integration_health_status IN ('HEALTHY', 'DEGRADED', 'UNKNOWN', 'NEVER_CONNECTED'));
