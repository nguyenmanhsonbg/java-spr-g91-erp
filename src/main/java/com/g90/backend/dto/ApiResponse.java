package com.g90.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

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
                .code(code)
                .message(message)
                .errors(errors == null || errors.isEmpty() ? null : errors)
                .build();
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, null);
    }
}
