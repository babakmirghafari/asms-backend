package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum SessionStatus implements ConvertableEnum {
    ACTIVE(1),
    EXPIRED(2),
    REVOKED(3);

    private final int key;

    SessionStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
