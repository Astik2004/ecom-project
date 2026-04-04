package com.astik.user_service.service.usecase.impl;

import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.enums.UserStatus;
import com.astik.user_service.exception.UserAlreadyExistsException;
import com.astik.user_service.kafkaevent.EmailVerificationEvent;
import com.astik.user_service.kafkaevent.UserEventProducer;
import com.astik.user_service.kafkaevent.UserRegisteredEvent;
import com.astik.user_service.mapper.UserMapper;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.usecase.AuthHelper;
import com.astik.user_service.service.usecase.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserEventProducer userEventProducer;
    private final AuditService auditService;
    private final AuthHelper authHelper;

    @Value("${application.account.email-token-expiry-hours:24}")
    private int emailTokenExpiryHours;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }
        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);

        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(emailTokenExpiryHours));

        User saved = userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(saved);

        String accessToken = jwtUtils.generateAccessToken(principal);
        String refreshTokenStr = jwtUtils.generateRefreshToken(principal);
        authHelper.persistRefreshToken(saved, refreshTokenStr);

        userEventProducer.publishUserRegistered(
                UserRegisteredEvent.of(
                        saved.getId(), saved.getFirstName(),
                        saved.getLastName(), saved.getEmail(),
                        saved.getRole().name()));

        userEventProducer.publishEmailVerification(
                EmailVerificationEvent.of(
                        saved.getId(), saved.getEmail(),
                        saved.getFirstName(), verificationToken,
                        saved.getEmailVerificationTokenExpiry()));

        auditService.log(AuditAction.USER_REGISTERED, "User",
                saved.getId(), saved.getEmail(), null, null, true, null);

        log.info("User registered | userId={} email={}", saved.getId(), saved.getEmail());

        return authHelper.buildAuthResponse(accessToken, refreshTokenStr, principal);
    }
}
