package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PasswordResetTokenInvalidException extends ApiException {

    public PasswordResetTokenInvalidException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Reset password token is invalid or expired");
    }
}
