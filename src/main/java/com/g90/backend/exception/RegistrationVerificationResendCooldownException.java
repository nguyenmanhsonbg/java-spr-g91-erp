package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class RegistrationVerificationResendCooldownException extends ApiException {

    public RegistrationVerificationResendCooldownException(long retryAfterSeconds) {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                "REGISTRATION_VERIFICATION_RESEND_COOLDOWN",
                "Please wait " + retryAfterSeconds + " seconds before requesting another verification code"
        );
    }
}
