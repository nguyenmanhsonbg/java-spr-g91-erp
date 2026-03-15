package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractApprovalNotAllowedException extends ApiException {

    public ContractApprovalNotAllowedException(String message) {
        super(HttpStatus.FORBIDDEN, "CONTRACT_APPROVAL_NOT_ALLOWED", message);
    }
}
