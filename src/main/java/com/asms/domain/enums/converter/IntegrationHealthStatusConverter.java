package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.IntegrationHealthStatus;
import jakarta.persistence.Converter;

@Converter
public class IntegrationHealthStatusConverter extends AbstractEnumConverter<IntegrationHealthStatus> {
    public IntegrationHealthStatusConverter() {
        super(IntegrationHealthStatus.class);
    }
}
