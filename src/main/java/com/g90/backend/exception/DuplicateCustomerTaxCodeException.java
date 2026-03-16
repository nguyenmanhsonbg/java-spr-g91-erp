package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCustomerTaxCodeException extends ApiException {

    public DuplicateCustomerTaxCodeException() {
        super(HttpStatus.BAD_REQUEST, "DUPLICATE_CUSTOMER_TAX_CODE", "Tax code already exists");
    }
}
