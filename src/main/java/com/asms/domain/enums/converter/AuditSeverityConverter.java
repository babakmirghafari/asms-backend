package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.AuditSeverity;

import jakarta.persistence.Converter;

@Converter
public class AuditSeverityConverter extends AbstractEnumConverter<AuditSeverity> {
    public AuditSeverityConverter() {
        super(AuditSeverity.class);
    }
}
