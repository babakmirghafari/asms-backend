package com.asms.domain.enums;

import com.asms.domain.converter.ConvertableEnum;

/**
 * Live integration health status of an application connector.
 *
 * <p>Per ARC42 §6 Flow 7 — application onboarding health check.
 * Keys map to SMALLINT values stored in the {@code applications.integration_health_status} column.
 * DB values: HEALTHY=1, DEGRADED=2, UNKNOWN=3, NEVER_CONNECTED=4
 */
public enum IntegrationHealthStatus implements ConvertableEnum {
    HEALTHY(1),
    DEGRADED(2),
    UNKNOWN(3),
    NEVER_CONNECTED(4);

    private final int key;

    IntegrationHealthStatus(int key) {
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }
}
