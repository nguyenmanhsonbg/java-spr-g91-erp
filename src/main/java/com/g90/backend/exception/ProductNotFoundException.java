package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found");
    }
}
