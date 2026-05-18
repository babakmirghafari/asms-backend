package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Lifecycle status of an organization.
 *
 * <p>Keys map to SMALLINT values stored in the {@code organizations.status} column.
 * DB values: ACTIVE=1, SUSPENDED=2, DELETED=3
 */
public enum OrganizationStatus implements ConvertableEnum {
    ACTIVE(1),
    SUSPENDED(2),
    DELETED(3);

    private final int key;

    OrganizationStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
