package com.g90.backend.exception;

import com.g90.backend.dto.ApiResponse;
import com.g90.backend.dto.ValidationErrorItem;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getCode(), exception.getMessage(), exception.getErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "Invalid request data", extractFieldErrors(exception.getBindingResult().getFieldErrors())));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "Invalid request data", extractFieldErrors(exception.getBindingResult().getFieldErrors())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        List<ValidationErrorItem> errors = exception.getConstraintViolations().stream()
                .map(violation -> ValidationErrorItem.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "Invalid request data", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        "VALIDATION_ERROR",
                        "Invalid request data",
                        List.of(ValidationErrorItem.builder()
                                .field("request")
                                .message("Request body is invalid")
                                .build())
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
    }

    private List<ValidationErrorItem> extractFieldErrors(List<FieldError> fieldErrors) {
        Map<String, String> uniqueErrors = new LinkedHashMap<>();
        for (FieldError fieldError : fieldErrors) {
            uniqueErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        List<ValidationErrorItem> errors = new ArrayList<>();
        uniqueErrors.forEach((field, message) -> errors.add(ValidationErrorItem.builder()
                .field(field)
                .message(message)
                .build()));
        return errors;
    }
}
