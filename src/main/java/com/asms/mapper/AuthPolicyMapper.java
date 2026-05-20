package com.asms.mapper;

import com.asms.domain.AuthPolicy;
import com.asms.model.AuthPolicyDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthPolicyMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "maxFailedLoginAttempts", source = "maxFailedAttempts")
    @Mapping(target = "mfaRequired", source = "requireMfa")
    @Mapping(target = "passwordRequiresUppercase", source = "passwordRequireUppercase")
    @Mapping(target = "passwordRequiresNumber", source = "passwordRequireNumbers")
    @Mapping(target = "passwordRequiresSpecial", source = "passwordRequireSymbols")
    @Mapping(target = "sessionTimeoutMinutes", source = "lockoutDurationMinutes")
    AuthPolicyDto toDto(AuthPolicy policy);
}
