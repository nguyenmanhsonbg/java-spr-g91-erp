package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class QuotationProjectAccessException extends ApiException {

    public QuotationProjectAccessException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_PROJECT", "Project is invalid or does not belong to the current customer");
    }
}
