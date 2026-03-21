package com.astik.notification_service.service;

import com.astik.notification_service.dto.EmailVerificationEvent;
import com.astik.notification_service.dto.PasswordResetEvent;
import com.astik.notification_service.dto.UserRegisteredEvent;

public interface EmailService {
    void sendWelcomeEmail(UserRegisteredEvent event);
    void sendVerificationEmail(EmailVerificationEvent event);
    void sendPasswordResetEmail(PasswordResetEvent event);
}