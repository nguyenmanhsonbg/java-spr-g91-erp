package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PromotionNotFoundException extends ApiException {

    public PromotionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROMOTION_NOT_FOUND", "Promotion not found");
    }
}
