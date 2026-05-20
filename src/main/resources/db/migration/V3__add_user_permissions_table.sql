-- Direct permission assignments for users (task: wire User ↔ Permission relationship).
-- Permissions can be granted directly to a user in addition to group membership.
-- This backs the @ManyToMany directPermissions field on the User entity.

SET search_path TO asms;

CREATE TABLE user_permissions (
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID        NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, permission_id)
);

CREATE INDEX idx_user_permissions_user       ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_permission ON user_permissions(permission_id);
