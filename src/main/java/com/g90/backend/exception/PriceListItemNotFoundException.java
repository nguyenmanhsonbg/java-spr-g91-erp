package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PriceListItemNotFoundException extends ApiException {

    public PriceListItemNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PRICE_LIST_ITEM_NOT_FOUND", "Price list item not found");
    }
}
