package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.AuthProvider;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.name", target = "role")
    @Mapping(source = "provider",  target = "provider")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName",  target = "lastName")
    @Mapping(source = "mfaEnabled", target = "mfaEnabled")
    UserDto toDto(User entity);

    @Mapping(source = "role.name", target = "role")
    @Mapping(source = "provider",  target = "provider")
    @Mapping(source = "createdAt", target = "createdAt")
    AdminUserDto toAdminDto(User entity);

    default String map(RoleName name) { return name != null ? name.name() : null; }
    default String map(AuthProvider p) { return p != null ? p.name() : null; }

    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "passwordHash",    ignore = true)
    @Mapping(target = "role",            ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "provider",        ignore = true)
    @Mapping(target = "mfaEnabled",      ignore = true)
    @Mapping(target = "mfaSecretEnc",    ignore = true)
    @Mapping(target = "mfaSecretTmpEnc", ignore = true)
    User toEntity(RegisterDto dto);
}
