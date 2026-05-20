package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.DeliveryMethod;
import com.asms.domain.enums.Department;
import jakarta.persistence.Converter;

@Converter
public class DeliveryMethodConverter extends AbstractEnumConverter<DeliveryMethod> {
    public DeliveryMethodConverter() {
        super(DeliveryMethod.class);
    }
}
