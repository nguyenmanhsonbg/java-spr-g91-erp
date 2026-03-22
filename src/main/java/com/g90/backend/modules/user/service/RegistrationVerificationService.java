package com.g90.backend.modules.user.service;

import com.g90.backend.modules.account.entity.UserAccountEntity;

public interface RegistrationVerificationService {

    VerificationDispatch issueForNewRegistration(UserAccountEntity user);

    VerificationDispatch resend(String email);

    VerificationCompletion verify(String email, String verificationCode);

    record VerificationDispatch(
            String userId,
            String email,
            String recipientName,
            String verificationCode,
            int expireMinutes
    ) {
    }

    record VerificationCompletion(
            String userId,
            String email,
            String status,
            boolean verified
    ) {
    }
}
