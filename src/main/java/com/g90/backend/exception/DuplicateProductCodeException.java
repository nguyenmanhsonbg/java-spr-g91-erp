package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class DuplicateProductCodeException extends ApiException {

    public DuplicateProductCodeException() {
        super(
                HttpStatus.CONFLICT,
                "PRODUCT_CODE_ALREADY_EXISTS",
                "Product code already exists",
                List.of(ValidationErrorItem.builder()
                        .field("productCode")
                        .message("Product code must be unique")
                        .build())
        );
    }
}
