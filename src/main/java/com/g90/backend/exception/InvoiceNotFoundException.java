package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends ApiException {

    public InvoiceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found");
    }
}
