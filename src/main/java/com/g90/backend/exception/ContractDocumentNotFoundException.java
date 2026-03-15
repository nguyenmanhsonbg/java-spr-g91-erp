package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractDocumentNotFoundException extends ApiException {

    public ContractDocumentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CONTRACT_DOCUMENT_NOT_FOUND", "Contract document not found");
    }
}
