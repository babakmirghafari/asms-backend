package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum UserStatus implements ConvertableEnum {
    PENDING_ACTIVATION(1),
    ACTIVE(2),
    INACTIVE(3),
    LOCKED(4),
    TEMP_PASSWORD(5),
    PENDING_MFA_ENROLLMENT(6),
    DELETED(7);

    private final int key;

    UserStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
