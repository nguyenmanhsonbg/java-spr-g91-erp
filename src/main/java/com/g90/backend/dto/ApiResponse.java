package com.g90.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final String DEFAULT_ERROR_CODE = "INTERNAL_SERVER_ERROR";
    private static final String DEFAULT_ERROR_MESSAGE = "An unexpected error occurred";

    private final String code;
    private final String message;
    private final T data;
    private final List<ValidationErrorItem> errors;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> error(String code, String message, List<ValidationErrorItem> errors) {
        return ApiResponse.<Void>builder()
                .code(hasText(code) ? code : DEFAULT_ERROR_CODE)
                .message(hasText(message) ? message : DEFAULT_ERROR_MESSAGE)
                .errors(errors == null || errors.isEmpty() ? null : errors)
                .build();
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
