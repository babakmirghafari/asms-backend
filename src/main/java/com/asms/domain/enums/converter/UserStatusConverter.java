package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.UserStatus;

import jakarta.persistence.Converter;

@Converter
public class UserStatusConverter extends AbstractEnumConverter<UserStatus> {
    public UserStatusConverter() {
        super(UserStatus.class);
    }
}
