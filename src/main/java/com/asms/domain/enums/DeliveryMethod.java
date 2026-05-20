package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum DeliveryMethod implements ConvertableEnum {
    Email(1),
    SMS(2),
    Both(3),
    HR(4),
    Manuel_Copy(5);

    private final int key;

    DeliveryMethod(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
