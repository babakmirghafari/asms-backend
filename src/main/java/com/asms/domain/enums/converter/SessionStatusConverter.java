package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.SessionStatus;

import jakarta.persistence.Converter;

@Converter
public class SessionStatusConverter extends AbstractEnumConverter<SessionStatus> {
    public SessionStatusConverter() {
        super(SessionStatus.class);
    }
}
