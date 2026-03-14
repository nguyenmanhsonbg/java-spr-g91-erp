package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class QuotationPricingNotFoundException extends ApiException {

    public QuotationPricingNotFoundException(String productId) {
        super(
                HttpStatus.BAD_REQUEST,
                "QUOTATION_PRICING_NOT_FOUND",
                "No valid price list is available for one or more products",
                List.of(ValidationErrorItem.builder()
                        .field("productId")
                        .message("No valid price found for product: " + productId)
                        .build())
        );
    }
}
