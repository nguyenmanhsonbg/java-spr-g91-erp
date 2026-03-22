package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class RegistrationVerificationNotPendingException extends ApiException {

    public RegistrationVerificationNotPendingException() {
        super(HttpStatus.NOT_FOUND, "REGISTRATION_VERIFICATION_NOT_PENDING", "No pending registration verification found for the provided email");
    }
}
