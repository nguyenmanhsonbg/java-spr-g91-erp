package com.g90.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidationErrorItem {

    private final String field;
    private final String message;
}
