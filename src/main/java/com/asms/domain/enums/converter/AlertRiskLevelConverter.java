package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.AlertRiskLevel;
import jakarta.persistence.Converter;

@Converter
public class AlertRiskLevelConverter extends AbstractEnumConverter<AlertRiskLevel> {
    public AlertRiskLevelConverter() {
        super(AlertRiskLevel.class);
    }
}
