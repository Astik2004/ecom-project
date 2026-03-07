package com.astik.user_service.service.impl;

import com.astik.user_service.kafkaevent.NotificationEventPublisher;
import com.astik.user_service.mapper.UserMapper;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;
    private final NotificationEventPublisher notificationPublisher;
    //private final TokenUtil tokenUtil;
    private final UserMapper userMapper;

    @Value("${application.account.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${application.account.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Value("${application.account.email-token-expiry-hours:24}")
    private int emailTokenExpiryHours;

    @Value("${application.account.password-reset-token-expiry-minutes:15}")
    private int resetTokenExpiryMinutes;


}
