package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Risk level classification of a security alert.
 *
 * <p>Keys map to SMALLINT values stored in the {@code alerts.risk_level} column.
 * DB values: LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4
 */
public enum AlertRiskLevel implements ConvertableEnum {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int key;

    AlertRiskLevel(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
