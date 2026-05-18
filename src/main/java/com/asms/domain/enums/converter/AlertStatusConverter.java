package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.AlertStatus;

import jakarta.persistence.Converter;

@Converter
public class AlertStatusConverter extends AbstractEnumConverter<AlertStatus> {
    public AlertStatusConverter() {
        super(AlertStatus.class);
    }
}
