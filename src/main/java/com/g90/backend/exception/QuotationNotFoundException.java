package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class QuotationNotFoundException extends ApiException {

    public QuotationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "Requested resource not found");
    }
}
