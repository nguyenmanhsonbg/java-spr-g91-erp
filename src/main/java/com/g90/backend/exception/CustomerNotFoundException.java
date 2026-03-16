package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends ApiException {

    public CustomerNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found");
    }
}
