package com.asms.domain.enums;

/**
 * Permission lifecycle status values.
 * Lifecycle: DRAFT → ACTIVE → DEPRECATED (no reverse transitions).
 */
public enum PermissionStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED
}
