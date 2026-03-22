package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class RegistrationAlreadyVerifiedException extends ApiException {

    public RegistrationAlreadyVerifiedException() {
        super(HttpStatus.CONFLICT, "REGISTRATION_ALREADY_VERIFIED", "The account has already been verified");
    }
}
