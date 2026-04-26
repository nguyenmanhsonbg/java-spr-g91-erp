package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class QuotationNotFoundException extends ApiException {

    public QuotationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "QUOTATION_NOT_FOUND", "Quotation not found");
    }
}
