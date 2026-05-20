package com.asms.mapper;

import com.asms.domain.Membership;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.UserRole;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.model.MembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MembershipMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "status", source = "status")
    MembershipDto toDto(Membership membership);

    @ValueMapping(source = "PENDING", target = MappingConstants.NULL)
    @ValueMapping(source = "REMOVED", target = MappingConstants.NULL)
    MembershipDto.StatusEnum toStatusEnum(MembershipStatus status);

    // Scalar fields only — service must set user and organization from their repositories.
    @Named("toMembershipEntity")
    default Membership toMembershipEntity(CreateMembershipRequestDto dto) {
        if (dto == null) return Membership.builder().build();
        return Membership.builder()
                .role(UserRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
