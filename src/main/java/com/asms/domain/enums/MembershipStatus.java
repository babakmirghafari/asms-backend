package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum MembershipStatus implements ConvertableEnum {
    PENDING(1),
    ACTIVE(2),
    SUSPENDED(3),
    REMOVED(4);

    private final int key;

    MembershipStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
