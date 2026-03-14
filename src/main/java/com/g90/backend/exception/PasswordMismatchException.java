package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends ApiException {

    public PasswordMismatchException(String field, String message) {
        super(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_MISMATCH",
                message,
                List.of(ValidationErrorItem.builder()
                        .field(field)
                        .message(message)
                        .build())
        );
    }
}
