package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class PricingProductNotFoundException extends ApiException {

    public PricingProductNotFoundException() {
        super(
                HttpStatus.NOT_FOUND,
                "MSG25",
                "Product not found",
                List.of(ValidationErrorItem.builder()
                        .field("productId")
                        .message("Product not found")
                        .build())
        );
    }
}
