package com.asms.domain.enums;

/**
 * User lifecycle status values.
 * Stored as VARCHAR in the database via {@code @Enumerated(EnumType.STRING)}.
 */
public enum UserStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    INACTIVE,
    LOCKED,
    TEMP_PASSWORD,
    PENDING_MFA_ENROLLMENT,
    DELETED
}
