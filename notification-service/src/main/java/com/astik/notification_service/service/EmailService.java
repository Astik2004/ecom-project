package com.astik.notification_service.service;

import com.astik.notification_service.dto.UserRegisteredEvent;

public interface EmailService {
    void sendWelcomeEmail(UserRegisteredEvent event);
    void sendPasswordResetEmail(String email, String resetToken);
}