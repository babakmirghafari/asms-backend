package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.ApplicationStatus;
import jakarta.persistence.Converter;

@Converter
public class ApplicationStatusConverter extends AbstractEnumConverter<ApplicationStatus> {
    public ApplicationStatusConverter() {
        super(ApplicationStatus.class);
    }
}
