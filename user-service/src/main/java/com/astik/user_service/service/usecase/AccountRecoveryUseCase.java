package com.astik.user_service.service.usecase;

import com.astik.user_service.dto.request.ResetPasswordRequest;

public interface AccountRecoveryUseCase {
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
}
