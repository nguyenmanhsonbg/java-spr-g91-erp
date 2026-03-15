package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractNotFoundException extends ApiException {

    public ContractNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CONTRACT_NOT_FOUND", "Contract not found");
    }
}
