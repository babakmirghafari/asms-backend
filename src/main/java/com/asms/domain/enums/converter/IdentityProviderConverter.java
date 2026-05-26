package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.IdentityProvider;
import jakarta.persistence.Converter;

@Converter
public class IdentityProviderConverter extends AbstractEnumConverter<IdentityProvider> {
    public IdentityProviderConverter() {
        super(IdentityProvider.class);
    }
}
