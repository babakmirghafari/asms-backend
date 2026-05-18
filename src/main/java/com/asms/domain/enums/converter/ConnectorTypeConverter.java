package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.ConnectorType;
import jakarta.persistence.Converter;

@Converter
public class ConnectorTypeConverter extends AbstractEnumConverter<ConnectorType> {
    public ConnectorTypeConverter() {
        super(ConnectorType.class);
    }
}
