package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class CustomerProfileNotFoundException extends ApiException {

    public CustomerProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_PROFILE_NOT_FOUND", "Customer profile not found");
    }
}
