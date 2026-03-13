package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class RequestValidationException extends ApiException {

    public RequestValidationException(List<ValidationErrorItem> errors) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request data", errors);
    }

    public static RequestValidationException singleError(String field, String message) {
        return new RequestValidationException(List.of(
                ValidationErrorItem.builder()
                        .field(field)
                        .message(message)
                        .build()
        ));
    }
}
