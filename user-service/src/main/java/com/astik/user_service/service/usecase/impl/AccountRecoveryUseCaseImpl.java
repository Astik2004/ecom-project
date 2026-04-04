package com.astik.user_service.service.usecase.impl;

import com.astik.user_service.dto.request.ResetPasswordRequest;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.exception.InvalidTokenException;
import com.astik.user_service.exception.UserNotFoundException;
import com.astik.user_service.kafkaevent.EmailVerificationEvent;
import com.astik.user_service.kafkaevent.PasswordResetEvent;
import com.astik.user_service.kafkaevent.UserEventProducer;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.usecase.AccountRecoveryUseCase;
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
public class AccountRecoveryUseCaseImpl implements AccountRecoveryUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;
    private final AuditService auditService;

    @Value("${application.account.email-token-expiry-hours:24}")
    private int emailTokenExpiryHours;

    @Value("${application.account.password-reset-token-expiry-minutes:15}")
    private int passwordResetTokenExpiryMinutes;

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token expired");
        }

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalStateException("Email already verified");
        }

        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        auditService.log(AuditAction.EMAIL_VERIFIED, "User", user.getId(), user.getEmail(), null, null, true, null);
        log.info("Email verified | userId={}", user.getId());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalStateException("Email already verified");
        }

        String newToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(newToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(emailTokenExpiryHours));
        userRepository.save(user);

        userEventProducer.publishEmailVerification(
                EmailVerificationEvent.of(user.getId(), user.getEmail(), user.getFirstName(), newToken, user.getEmailVerificationTokenExpiry()));

        log.info("Verification email resent | userId={}", user.getId());
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(passwordResetTokenExpiryMinutes));
            userRepository.save(user);

            userEventProducer.publishPasswordReset(
                    PasswordResetEvent.of(user.getId(), user.getEmail(), user.getFirstName(), resetToken, user.getPasswordResetTokenExpiry()));

            auditService.log(AuditAction.PASSWORD_RESET_REQUESTED, "User", user.getId(), user.getEmail(), null, null, true, null);
            log.info("Password reset requested | userId={}", user.getId());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Password reset token expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);

        refreshTokenRepository.revokeAllUserTokens(user);
        userRepository.save(user);

        auditService.log(AuditAction.PASSWORD_CHANGED, "User", user.getId(), user.getEmail(), null, null, true, null);
        log.info("Password reset successful | userId={}", user.getId());
    }
}
