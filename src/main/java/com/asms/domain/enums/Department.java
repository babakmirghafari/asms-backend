package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum Department implements ConvertableEnum {
    Finance(1),
    IT_Security(2),
    Operations(3),
    HR(4),
    Compliance(5),
    Engineering(6),
    Customer_Support(7);

    private final int key;

    Department(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
