package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractCancelNotAllowedException extends ApiException {

    public ContractCancelNotAllowedException(String message) {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_CANCEL_NOT_ALLOWED", message);
    }
}
