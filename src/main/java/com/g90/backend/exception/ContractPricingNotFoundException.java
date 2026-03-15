package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractPricingNotFoundException extends ApiException {

    public ContractPricingNotFoundException(String productId) {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_PRICING_NOT_FOUND", "No active pricing found for product: " + productId);
    }
}
