package com.asms.domain;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Lifecycle status of a {@link PermissionImport} session (ARC42 §6 Flow 6 — two-step import).
 *
 * <p>Status transitions:
 * <pre>
 *   PENDING_COMMIT  → COMMITTED  (commit step succeeds)
 *   PENDING_COMMIT  → EXPIRED    (TTL elapsed before commit)
 *   BLOCKED         → (terminal — re-upload required)
 * </pre>
 */
public enum PermissionImportStatus implements ConvertableEnum {

    PENDING_COMMIT(1),
    BLOCKED(2),
    COMMITTED(3),
    EXPIRED(4);

    private final int key;

    PermissionImportStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
