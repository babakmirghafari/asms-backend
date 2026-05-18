package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.AlertSeverity;

import jakarta.persistence.Converter;

@Converter
public class AlertSeverityConverter extends AbstractEnumConverter<AlertSeverity> {
    public AlertSeverityConverter() {
        super(AlertSeverity.class);
    }
}
