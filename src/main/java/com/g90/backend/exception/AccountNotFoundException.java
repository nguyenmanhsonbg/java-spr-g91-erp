package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "User account not found");
    }
}
