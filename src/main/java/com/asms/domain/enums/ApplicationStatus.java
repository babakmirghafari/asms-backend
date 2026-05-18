package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Registration lifecycle status of a client application.
 *
 * <p>Keys map to SMALLINT values stored in the {@code applications.status} column.
 * DB values: ACTIVE=1, INACTIVE=2, SUSPENDED=3, DELETED=4
 */
public enum ApplicationStatus implements ConvertableEnum {
    ACTIVE(1),
    INACTIVE(2),
    SUSPENDED(3),
    DELETED(4);

    private final int key;

    ApplicationStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
