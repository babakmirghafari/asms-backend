package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Lifecycle status of a station policy.
 *
 * <p>Keys map to SMALLINT values stored in the {@code station_policies.status} column.
 * DB values: ACTIVE=1, INACTIVE=2
 */
public enum StationPolicyStatus implements ConvertableEnum {
    ACTIVE(1),
    INACTIVE(2);

    private final int key;

    StationPolicyStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
