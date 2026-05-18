package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.StationPolicyStatus;
import jakarta.persistence.Converter;

@Converter
public class StationPolicyStatusConverter extends AbstractEnumConverter<StationPolicyStatus> {
    public StationPolicyStatusConverter() {
        super(StationPolicyStatus.class);
    }
}
