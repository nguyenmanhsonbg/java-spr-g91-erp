package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class QuotationNotEditableException extends ApiException {

    public QuotationNotEditableException() {
        super(HttpStatus.BAD_REQUEST, "QUOTATION_NOT_EDITABLE", "Only draft quotation can be edited");
    }
}
