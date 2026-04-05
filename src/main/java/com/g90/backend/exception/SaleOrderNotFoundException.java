package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class SaleOrderNotFoundException extends ApiException {

    public SaleOrderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SALE_ORDER_NOT_FOUND", "Sale order not found");
    }
}
