package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class QuotationAlreadyConvertedException extends ApiException {

    public QuotationAlreadyConvertedException() {
        super(HttpStatus.BAD_REQUEST, "QUOTATION_ALREADY_CONVERTED", "Quotation has already been converted to contract");
    }
}
