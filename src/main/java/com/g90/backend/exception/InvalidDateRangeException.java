package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApiException {

    public InvalidDateRangeException() {
        super(
                HttpStatus.BAD_REQUEST,
                "INVALID_DATE_RANGE",
                "Invalid date range",
                List.of(ValidationErrorItem.builder()
                        .field("validFrom")
                        .message("Valid from must be on or before valid to")
                        .build())
        );
    }
}
