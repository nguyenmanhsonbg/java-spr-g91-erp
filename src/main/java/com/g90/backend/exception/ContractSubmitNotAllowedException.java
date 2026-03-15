package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractSubmitNotAllowedException extends ApiException {

    public ContractSubmitNotAllowedException(String message) {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_SUBMIT_NOT_ALLOWED", message);
    }
}
