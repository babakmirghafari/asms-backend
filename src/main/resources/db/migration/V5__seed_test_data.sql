SET search_path TO asms;

-- ─── TEST SEED DATA (dev/test only) ─────────────────────────────────────────
-- Two organisations and two users to exercise every auth flow in the frontend.
--
-- Login rules (AuthService):
--   • password_hash IS NULL  → any password is accepted
--   • force_password_change  → TEMP_PASSWORD_REQUIRED flow
--   • mfa_enabled            → MFA_REQUIRED flow
--   • multi-org membership   → ORG_SELECTION_REQUIRED flow
--
-- Credentials summary:
--   admin     / <any password>  → org-select screen (two orgs)
--   mfa.user  / <any password>  → MFA screen (any 6 digits accepted, stubbed)

-- ─── ORGANISATIONS ──────────────────────────────────────────────────────────

INSERT INTO organizations (id, name, slug, display_name, status, created_at, updated_at)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Acme Corp',  'acme-corp',  'Acme Corporation', 1, now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'Beta LLC',   'beta-llc',   'Beta LLC',         1, now(), now())
ON CONFLICT DO NOTHING;

-- ─── USERS ──────────────────────────────────────────────────────────────────
-- status 2 = ACTIVE, force_password_change = false, delivery_method 1 = Email

INSERT INTO users (id, username, email, full_name, status, password_hash,
                   force_password_change, mfa_enabled, failed_login_attempts,
                   delivery_method, created_at, updated_at)
VALUES
    -- Triggers: ORG_SELECTION_REQUIRED (two org memberships, no MFA)
    ('bbbbbbbb-0000-0000-0000-000000000001',
     'admin', 'admin@acmecorp.com', 'Admin User',
     2, NULL, FALSE, FALSE, 0, 1, now(), now()),

    -- Triggers: MFA_REQUIRED (MFA enabled, single org)
    ('bbbbbbbb-0000-0000-0000-000000000002',
     'mfa.user', 'mfa@acmecorp.com', 'MFA User',
     2, NULL, FALSE, TRUE, 0, 1, now(), now())
ON CONFLICT DO NOTHING;

-- ─── MEMBERSHIPS ────────────────────────────────────────────────────────────
-- role 2 = ADMIN, role 4 = MEMBER, status 2 = ACTIVE

INSERT INTO memberships (id, user_id, org_id, role, status, created_at, updated_at)
VALUES
    -- admin is ADMIN in Acme Corp and MEMBER in Beta LLC → triggers org-select
    ('cccccccc-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
     2, 2, now(), now()),
    ('cccccccc-0000-0000-0000-000000000002',
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002',
     4, 2, now(), now()),

    -- mfa.user is MEMBER in Acme Corp only
    ('cccccccc-0000-0000-0000-000000000003',
     'bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
     4, 2, now(), now())
ON CONFLICT DO NOTHING;
