package com.astik.user_service.service.impl;

import com.astik.user_service.dto.request.UpdateProfileRequest;
import com.astik.user_service.dto.response.UserResponse;
import com.astik.user_service.entity.User;
import com.astik.user_service.exception.UserNotFoundException;
import com.astik.user_service.mapper.UserMapper;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper     userMapper;

    @Override
    public UserResponse getMyProfile(UserPrincipal principal) {
        // UserPrincipal already holds the User entity — no extra DB call needed
        return userMapper.toResponse(principal.getUser());
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(UserPrincipal principal,
                                        UpdateProfileRequest request) {
        // Re-fetch from DB to get managed entity for dirty tracking
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        userMapper.updateEntity(request, user);
        // No explicit save() needed — JPA dirty tracking handles it in @Transactional
        log.info("Profile updated | userId={}", user.getId());
        return userMapper.toResponse(user);
    }
}