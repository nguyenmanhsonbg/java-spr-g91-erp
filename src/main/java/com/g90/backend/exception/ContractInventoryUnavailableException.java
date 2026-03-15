package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractInventoryUnavailableException extends ApiException {

    public ContractInventoryUnavailableException(String message) {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_INVENTORY_UNAVAILABLE", message);
    }
}
