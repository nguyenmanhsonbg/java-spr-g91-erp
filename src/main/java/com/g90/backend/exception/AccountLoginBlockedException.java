package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class AccountLoginBlockedException extends ApiException {

    public AccountLoginBlockedException(String status) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_LOGIN_BLOCKED", "Account is " + status + " and cannot login");
    }
}
