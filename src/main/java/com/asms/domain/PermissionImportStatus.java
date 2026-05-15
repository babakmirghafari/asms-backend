package com.asms.domain;

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
public enum PermissionImportStatus {

    /**
     * CSV has been validated and passed (no errors). Ready to commit.
     * This status expires after {@code import.ttl-minutes} (default 30 min).
     */
    PENDING_COMMIT,

    /**
     * CSV validation found errors. Cannot be committed — caller must fix and re-upload.
     */
    BLOCKED,

    /**
     * Import has been successfully committed. Permissions have been written to the database.
     */
    COMMITTED,

    /**
     * Import session expired before commit. Re-upload required.
     */
    EXPIRED
}
