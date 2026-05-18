package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum UserRole implements ConvertableEnum {
    SUPER_ADMIN(1),
    ADMIN(2),
    SECURITY_ANALYST(3),
    MEMBER(4);

    private final int key;

    UserRole(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
