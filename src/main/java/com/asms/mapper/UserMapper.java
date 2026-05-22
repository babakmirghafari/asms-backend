package com.asms.mapper;

import com.asms.domain.User;
import com.asms.domain.enums.Department;
import com.asms.domain.enums.UserStatus;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.UpdateUserRequestDto;
import com.asms.model.UserDto;
import org.mapstruct.*;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "status", source = "status")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "displayName", ignore = true)
    @Mapping(target = "organizationIds", ignore = true)
    @Mapping(target = "workdays", ignore = true)
    @Mapping(target = "ipRestriction", ignore = true)
    @Mapping(target = "workHours", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserDto toDto(User user);

    // PENDING_ACTIVATION is a server-side pre-activation state — exposed as TEMP_PASSWORD to the UI
    // DELETED users are filtered out before mapping, but a null fallback avoids NPE in edge cases
    @ValueMapping(source = "PENDING_ACTIVATION", target = "TEMP_PASSWORD")
    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    UserDto.StatusEnum toStatusEnum(UserStatus status);

    default User toUserEntityFromCreateUserRequestDto(CreateUserRequestDto dto) {
        return toUser(dto);
    }

    @Named("toUserEntity")
    default User toUser(CreateUserRequestDto dto) {
        if (dto == null) return User.builder().build();
        return User.builder()
                .username(dto.getUsername())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .department(dto.getDepartment() != null ? Department.valueOf(dto.getDepartment()) : null)
                .build();
    }

    @Named("toUserPatch")
    default User toUserPatch(UpdateUserRequestDto dto) {
        if (dto == null) return User.builder().build();
        String fullName = Stream.of(dto.getFirstName(), dto.getLastName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .trim();
        return User.builder()
                .fullName(fullName.isEmpty() ? null : fullName)
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }
}
