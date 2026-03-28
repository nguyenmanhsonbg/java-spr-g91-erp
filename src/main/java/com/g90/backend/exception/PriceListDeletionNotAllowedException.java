package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PriceListDeletionNotAllowedException extends ApiException {

    public PriceListDeletionNotAllowedException() {
        super(
                HttpStatus.BAD_REQUEST,
                "PRICE_LIST_DELETE_NOT_ALLOWED",
                "Price list cannot be deleted because it is used by active orders"
        );
    }
}
