package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class CurrentPasswordIncorrectException extends ApiException {

    public CurrentPasswordIncorrectException() {
        super(
                HttpStatus.BAD_REQUEST,
                "CURRENT_PASSWORD_INCORRECT",
                "Current password is incorrect",
                List.of(ValidationErrorItem.builder()
                        .field("currentPassword")
                        .message("Current password is incorrect")
                        .build())
        );
    }
}
