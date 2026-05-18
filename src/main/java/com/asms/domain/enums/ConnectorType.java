package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Authentication protocol used by a registered client application.
 *
 * <p>Keys map to SMALLINT values stored in the {@code applications.type} column.
 * DB values: OIDC=1, SAML=2, API_TOKEN=3
 */
public enum ConnectorType implements ConvertableEnum {
    OIDC(1),
    SAML(2),
    API_TOKEN(3);

    private final int key;

    ConnectorType(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
