package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class CustomerAlreadyInactiveException extends ApiException {

    public CustomerAlreadyInactiveException() {
        super(HttpStatus.BAD_REQUEST, "CUSTOMER_ALREADY_INACTIVE", "Customer is already inactive");
    }
}
