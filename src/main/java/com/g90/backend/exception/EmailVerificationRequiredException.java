package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationRequiredException extends ApiException {

    public EmailVerificationRequiredException() {
        super(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED", "Email verification is required before login");
    }
}
