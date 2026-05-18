package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum AlertSeverity implements ConvertableEnum {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int key;

    AlertSeverity(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
