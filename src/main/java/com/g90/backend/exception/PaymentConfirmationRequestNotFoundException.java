package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PaymentConfirmationRequestNotFoundException extends ApiException {

    public PaymentConfirmationRequestNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PAYMENT_CONFIRMATION_REQUEST_NOT_FOUND", "Payment confirmation request not found");
    }
}
