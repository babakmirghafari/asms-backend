package com.asms.domain.enums.converter;

import com.asms.domain.PermissionImportStatus;
import com.asms.domain.converter.AbstractEnumConverter;

import jakarta.persistence.Converter;

@Converter
public class PermissionImportStatusConverter extends AbstractEnumConverter<PermissionImportStatus> {
    public PermissionImportStatusConverter() {
        super(PermissionImportStatus.class);
    }
}
