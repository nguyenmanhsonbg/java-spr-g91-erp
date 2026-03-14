package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApiException {

    public InvalidDateRangeException() {
        super(
                HttpStatus.BAD_REQUEST,
                "MSG50",
                "Invalid date range",
                List.of(ValidationErrorItem.builder()
                        .field("startDate")
                        .message("Start date must be before end date")
                        .build())
        );
    }
}
