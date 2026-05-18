package com.asms.mapper;

import com.asms.domain.Membership;
import com.asms.domain.enums.MembershipStatus;
import com.asms.model.MembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link Membership} domain entity ↔ {@link MembershipDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MembershipMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status", qualifiedByName = "membershipStatusToEnum")
    MembershipDto toDto(Membership membership);

    @Named("membershipStatusToEnum")
    static MembershipDto.StatusEnum membershipStatusToEnum(MembershipStatus status) {
        if (status == null) return null;
        try {
            return MembershipDto.StatusEnum.fromValue(status.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
