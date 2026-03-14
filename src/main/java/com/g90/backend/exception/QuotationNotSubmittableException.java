package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class QuotationNotSubmittableException extends ApiException {

    public QuotationNotSubmittableException() {
        super(HttpStatus.BAD_REQUEST, "QUOTATION_NOT_SUBMITTABLE", "Only draft quotation can be submitted");
    }
}
