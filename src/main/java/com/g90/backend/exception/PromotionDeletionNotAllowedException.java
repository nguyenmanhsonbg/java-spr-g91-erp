package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class PromotionDeletionNotAllowedException extends ApiException {

    public PromotionDeletionNotAllowedException() {
        super(
                HttpStatus.BAD_REQUEST,
                "PROMOTION_DELETION_NOT_ALLOWED",
                "Promotion cannot be deleted because it is applied to active orders"
        );
    }
}
