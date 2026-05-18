package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.MembershipStatus;

import jakarta.persistence.Converter;

@Converter
public class MembershipStatusConverter extends AbstractEnumConverter<MembershipStatus> {
    public MembershipStatusConverter() {
        super(MembershipStatus.class);
    }
}
