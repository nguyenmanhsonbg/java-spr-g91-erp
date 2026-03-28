package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class InsufficientInventoryException extends ApiException {

    public InsufficientInventoryException() {
        super(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_INVENTORY",
                "Insufficient inventory available",
                List.of(ValidationErrorItem.builder()
                        .field("quantity")
                        .message("Requested quantity exceeds available stock")
                        .build())
        );
    }
}
