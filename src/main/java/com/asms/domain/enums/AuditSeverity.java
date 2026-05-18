package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum AuditSeverity implements ConvertableEnum {
    INFO(1),
    WARNING(2),
    CRITICAL(3);

    private final int key;

    AuditSeverity(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
