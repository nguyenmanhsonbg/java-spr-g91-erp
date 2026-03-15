package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractCreditLimitExceededException extends ApiException {

    public ContractCreditLimitExceededException() {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_CREDIT_LIMIT_EXCEEDED", "Customer credit limit would be exceeded");
    }
}
