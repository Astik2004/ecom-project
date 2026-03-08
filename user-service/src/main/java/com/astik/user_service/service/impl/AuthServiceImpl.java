package com.astik.user_service.service.impl;

import com.astik.user_service.dto.AuthResponse;
import com.astik.user_service.dto.ForgotPasswordRequest;
import com.astik.user_service.dto.LoginRequest;
import com.astik.user_service.dto.RegisterRequest;
import com.astik.user_service.dto.ResetPasswordRequest;
import com.astik.user_service.entity.RefreshToken;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.enums.Role;
import com.astik.user_service.enums.UserStatus;
import com.astik.user_service.exception.ResourceNotFoundException;
import com.astik.user_service.exception.UserAlreadyExistsException;
import com.astik.user_service.mapper.UserMapper;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.AuthService;
import com.astik.user_service.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final AuditService auditService;
    private final TokenUtil tokenUtil;
    private final UserMapper userMapper;

    @Value("${application.account.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${application.account.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Value("${application.account.email-token-expiry-hours:24}")
    private int emailTokenExpiryHours;

    @Value("${application.account.password-reset-token-expiry-minutes:15}")
    private int resetTokenExpiryMinutes;

    // ─── Register ─────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            auditService.logFailure( email, AuditAction.CREATE, "USER", "Email already exists", ip);
            throw new UserAlreadyExistsException("Registration failed. Please try a different email.");
        }

        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            auditService.logFailure( email, AuditAction.CREATE, "USER", "Phone number already exists", ip);
            throw new UserAlreadyExistsException("Registration failed. Please try a different phone number.");
        }

        String verificationToken = tokenUtil.generateSecureToken();

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .role(Role.CUSTOMER)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .failedLoginAttempts(0)
                .emailVerificationToken(verificationToken)
                .emailVerificationTokenExpiry(LocalDateTime.now().plusHours(emailTokenExpiryHours))
                .build();
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        User saved = userRepository.save(user);
        log.info("New customer registered: {} | ip={}", email, ip);

        auditService.logSuccess(email, AuditAction.CREATE, "USER", saved.getId(), ip);

        return createAuthResponse(saved, "Registration successful. Please verify your email.");
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.email().toLowerCase().trim();
        String ip = resolveIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            auditService.logFailure( email, AuditAction.LOGIN, "USER", "User not found", ip);
            return new ResourceNotFoundException("Invalid email or password");
        });

        if (user.getStatus() == UserStatus.SUSPENDED) {
            auditService.logFailure( email, AuditAction.LOGIN, "USER", "Account is suspended/locked", ip);
            throw new LockedException("Account is temporarily locked. Try again later.");
        }

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

            // Successful Login
            user.setFailedLoginAttempts(0);
            userRepository.save(user); // Custom update query hata di, directly entity save kar di

            auditService.logWithDetails(email, AuditAction.LOGIN, "USER", user.getId(), null, user.toString(), null, ip, userAgent);
            log.info("User logged in: {} | ip={}", email, ip);

            return createAuthResponse(user, "Login successful");

        } catch (BadCredentialsException e) {
            handleFailedLogin(user, email, ip);
            throw new BadCredentialsException("Invalid email or password");
        } catch (DisabledException | LockedException e) {
            auditService.logFailure(email, AuditAction.LOGIN, "USER", e.getMessage(), ip);
            throw e;
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    public void logout(String refreshToken, HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
                rt.setRevoked(true);
                rt.setExpired(true);
                refreshTokenRepository.save(rt);

                auditService.logSuccess(rt.getUser().getEmail(),
                        AuditAction.UPDATE, "AUTH", rt.getUser().getId(), ip);
            });
        }
        log.info("User logged out successfully");
    }

    // ─── Verify Email ─────────────────────────────────────────────────────────

    public void verifyEmail(String token, HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid email verification token."));

        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Email verification token has expired.");
        }

        user.setIsEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        auditService.logSuccess(user.getEmail(), AuditAction.UPDATE, "USER", user.getId(), ip);
        log.info("Email verified for: {}", user.getEmail());
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void handleFailedLogin(User user, String email, String ip) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        auditService.logFailure(email, AuditAction.LOGIN, "USER", "Invalid password. Attempt: " + newAttempts, ip);

        if (newAttempts >= maxFailedAttempts) {
            user.setStatus(UserStatus.SUSPENDED);
            log.warn("Account locked: {} after {} failed attempts", email, newAttempts);

            auditService.logWithDetails( "SYSTEM", AuditAction.ACCOUNT_LOCKED,
                    "USER", user.getId(), "STATUS:ACTIVE", "STATUS:SUSPENDED", "status", ip, null);
        }
        userRepository.save(user);
    }

    private AuthResponse createAuthResponse(User user, String message) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = jwtUtil.generateRefreshToken(principal);

        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessExpiration())
                .user(userMapper.toResponse(user))
                .message(message)
                .build();
    }

    private void saveRefreshToken(User user, String token) {
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .expired(false)
                .build());
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}