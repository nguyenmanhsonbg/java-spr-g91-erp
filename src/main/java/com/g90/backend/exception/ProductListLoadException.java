package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProductListLoadException extends ApiException {

    public ProductListLoadException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "PRODUCT_LIST_LOAD_FAILED", "Unable to load product list");
    }
}
