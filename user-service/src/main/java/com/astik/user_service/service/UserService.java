package com.astik.user_service.service;

import com.astik.user_service.dto.request.UpdateProfileRequest;
import com.astik.user_service.dto.response.UserResponse;
import com.astik.user_service.security.UserPrincipal;

public interface UserService {
    UserResponse getMyProfile(UserPrincipal principal);
    UserResponse updateMyProfile(UserPrincipal principal,
                                 UpdateProfileRequest request);
}