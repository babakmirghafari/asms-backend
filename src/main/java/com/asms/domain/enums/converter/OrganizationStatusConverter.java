package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.OrganizationStatus;
import jakarta.persistence.Converter;

@Converter
public class OrganizationStatusConverter extends AbstractEnumConverter<OrganizationStatus> {
    public OrganizationStatusConverter() {
        super(OrganizationStatus.class);
    }
}
