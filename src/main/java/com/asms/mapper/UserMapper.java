package com.asms.mapper;

import com.asms.domain.User;
import com.asms.model.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link User} domain entity ↔ {@link UserDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via {@code @Autowired}
 * or constructor injection. Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a {@link User} entity to a {@link UserDto}.
     *
     * <p>The {@code status} field in domain is a {@link String}; in the DTO it is
     * {@link UserDto.StatusEnum}. MapStruct converts via the named method below.
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    UserDto toDto(User user);

    @Named("stringToStatusEnum")
    static UserDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return UserDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
