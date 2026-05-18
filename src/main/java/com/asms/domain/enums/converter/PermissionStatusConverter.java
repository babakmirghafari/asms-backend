package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.PermissionStatus;

import jakarta.persistence.Converter;

@Converter
public class PermissionStatusConverter extends AbstractEnumConverter<PermissionStatus> {
    public PermissionStatusConverter() {
        super(PermissionStatus.class);
    }
}
