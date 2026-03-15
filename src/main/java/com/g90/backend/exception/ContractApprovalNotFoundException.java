package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractApprovalNotFoundException extends ApiException {

    public ContractApprovalNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CONTRACT_APPROVAL_NOT_FOUND", "Pending contract approval not found");
    }
}
