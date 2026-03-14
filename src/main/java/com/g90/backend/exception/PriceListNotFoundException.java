package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PriceListNotFoundException extends ApiException {

    public PriceListNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PRICE_LIST_NOT_FOUND", "Price list not found");
    }
}
