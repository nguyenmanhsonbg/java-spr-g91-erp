package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractNotEditableException extends ApiException {

    public ContractNotEditableException() {
        super(HttpStatus.BAD_REQUEST, "CONTRACT_NOT_EDITABLE", "Only draft contract can be edited");
    }
}
