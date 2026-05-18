package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.UserRole;

import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter extends AbstractEnumConverter<UserRole> {
    public UserRoleConverter() {
        super(UserRole.class);
    }
}
