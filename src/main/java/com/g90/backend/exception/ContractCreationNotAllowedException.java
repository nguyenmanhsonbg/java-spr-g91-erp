package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractCreationNotAllowedException extends ApiException {

    public ContractCreationNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_CREATION_NOT_ALLOWED", "Quotation is not eligible for contract creation");
    }
}
