-- V3 Contract Column Changes — ASMS API Contract v2.0.0
-- 1. permissions:   collapse application+module into resource (v2 PermissionDto.resource)
-- 2. applications:  add saml_entity_id (v2 ApplicationDto.samlEntityId)
-- 3. station_policies: add name/description/status columns; make user_id nullable
--                      (v2 StationPolicyDto adds name/description/status; policies are org-scoped)

-- ─── PERMISSIONS: replace application+module with resource ───────────────────

ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS resource VARCHAR(255);

-- Populate resource from existing application/module for any existing rows
UPDATE permissions
    SET resource = application || '/' || module
    WHERE resource IS NULL;

-- Apply NOT NULL after population
ALTER TABLE permissions
    ALTER COLUMN resource SET NOT NULL;

-- Drop old columns and their index
DROP INDEX IF EXISTS idx_permissions_application;

ALTER TABLE permissions
    DROP COLUMN IF EXISTS application,
    DROP COLUMN IF EXISTS module;

-- New index on resource
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions(org_id, resource);

-- ─── APPLICATIONS: add saml_entity_id ────────────────────────────────────────

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS saml_entity_id VARCHAR(255);

-- ─── STATION POLICIES: add name/description/status; make user_id nullable ────

ALTER TABLE station_policies
    ADD COLUMN IF NOT EXISTS name        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Populate name for existing rows (required field)
UPDATE station_policies
    SET name = 'Policy-' || id::text
    WHERE name IS NULL;

ALTER TABLE station_policies
    ALTER COLUMN name SET NOT NULL;

-- Make user_id nullable: v2 station policies are org-scoped, not user-scoped
ALTER TABLE station_policies
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE station_policies
    ADD CONSTRAINT chk_station_policy_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'));

-- ─── INET → TEXT: map ip_address columns to text for JPA compatibility ────────
-- PostgreSQL inet validates IP address format but Hibernate 6 has no built-in
-- inet JDBC type mapping; storing as text avoids driver-level type mismatch.
-- Application-level validation replaces DB-level inet type checking.

ALTER TABLE audit_logs
    ALTER COLUMN ip_address TYPE TEXT USING ip_address::text;

ALTER TABLE sessions
    ALTER COLUMN ip_address TYPE TEXT USING ip_address::text;
