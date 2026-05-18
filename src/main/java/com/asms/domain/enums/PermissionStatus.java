package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Permission lifecycle status values.
 * Lifecycle: DRAFT → ACTIVE → DEPRECATED (no reverse transitions).
 */
public enum PermissionStatus implements ConvertableEnum {
    DRAFT(1),
    ACTIVE(2),
    DEPRECATED(3);

    private final int key;

    PermissionStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
