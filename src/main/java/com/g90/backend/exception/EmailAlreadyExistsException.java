package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException() {
        super(
                HttpStatus.CONFLICT,
                "MSG21",
                "Email already exists",
                List.of(ValidationErrorItem.builder()
                        .field("email")
                        .message("Email must be unique")
                        .build())
        );
    }
}
