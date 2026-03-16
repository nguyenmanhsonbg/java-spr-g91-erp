package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class CustomerDisableNotAllowedException extends ApiException {

    public CustomerDisableNotAllowedException(String message) {
        super(HttpStatus.BAD_REQUEST, "CUSTOMER_DISABLE_NOT_ALLOWED", message);
    }
}
