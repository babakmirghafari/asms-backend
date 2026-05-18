package com.asms.mapper;

import com.asms.domain.User;
import com.asms.domain.enums.UserStatus;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.UpdateUserRequestDto;
import com.asms.model.UserDto;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link User} domain entity ↔ {@link UserDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via {@code @Autowired}
 * or constructor injection. Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "status", source = "status")
    UserDto toDto(User user);

    @ValueMapping(source = "PENDING_ACTIVATION", target = MappingConstants.NULL)
    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    UserDto.StatusEnum toStatusEnum(UserStatus status);

    default User toUserEntityFromCreateUserRequestDto(CreateUserRequestDto createUserRequestDto) {
        return toUser(createUserRequestDto);
    }

    @Named("toUserEntity")
    default User toUser(CreateUserRequestDto dto) {
        if (dto == null) return User.builder().build();
        return User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }

    /**
     * Converts an {@link UpdateUserRequestDto} into a partial {@link User} patch object.
     * Only non-null DTO fields are populated; the service applies them selectively.
     *
     * @param dto the incoming update request
     * @return a partial User carrying the fields to update
     */
    @Named("toUserPatch")
    default User toUserPatch(UpdateUserRequestDto dto) {
        if (dto == null) return User.builder().build();
        return User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }
}
