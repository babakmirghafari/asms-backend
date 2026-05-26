package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Lifecycle identity of an organization.
 *
 * <p>Keys map to SMALLINT values stored in the {@code organizations.identity} column.
 * DB values: OKTA=1, AZURE_AD=2, GOOGLE_WORKSPACE=3,AUTH0=4, ONE_LOGIN=5,PING_IDENTITY=6
 */
public enum IdentityProvider implements ConvertableEnum {
    OKTA(1),
    AZURE_AD(2),
    GOOGLE_WORKSPACE(3),
    AUTH0(4),
    ONE_LOGIN(5),
    PING_IDENTITY(6);


    private final int key;

    IdentityProvider(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
