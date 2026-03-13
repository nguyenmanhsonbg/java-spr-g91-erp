package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<ValidationErrorItem> errors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message, List<ValidationErrorItem> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }
}
