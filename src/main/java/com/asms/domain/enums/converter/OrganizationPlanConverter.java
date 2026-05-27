package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.OrganizationPlan;
import com.asms.domain.enums.OrganizationStatus;
import jakarta.persistence.Converter;

@Converter
public class OrganizationPlanConverter extends AbstractEnumConverter<OrganizationPlan> {
    public OrganizationPlanConverter() {
        super(OrganizationPlan.class);
    }
}
