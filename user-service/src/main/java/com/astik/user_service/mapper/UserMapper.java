package com.astik.user_service.mapper;

import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.request.UpdateProfileRequest;
import com.astik.user_service.dto.response.UserResponse;
import com.astik.user_service.entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserResponse toResponse(User user);


    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterRequest request);


    @Mapping(target = "email",    ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role",     ignore = true)
    void updateEntity(UpdateProfileRequest request, @MappingTarget User user);
}