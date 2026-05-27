package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Lifecycle status of an organization.
 *
 * <p>Keys map to SMALLINT values stored in the {@code organizations.status} column.
 * DB values: Starter=1, Professional=2, Enterprise=3
 */
public enum OrganizationPlan implements ConvertableEnum {
    STARTER(1),
    PROFESSIONAL(2),
    ENTERPRISE(3);

    private final int key;

    OrganizationPlan(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
