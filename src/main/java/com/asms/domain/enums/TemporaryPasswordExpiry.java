package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

public enum TemporaryPasswordExpiry implements ConvertableEnum {
    One_Hour(1),
    TwentyFour_Hours(2),
    Three_days(3),
    Seven_days(7);

    private final int key;

    TemporaryPasswordExpiry(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
