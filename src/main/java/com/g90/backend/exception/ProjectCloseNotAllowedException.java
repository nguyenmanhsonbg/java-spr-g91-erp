package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectCloseNotAllowedException extends ApiException {

    public ProjectCloseNotAllowedException(String message) {
        super(HttpStatus.BAD_REQUEST, "PROJECT_CLOSE_NOT_ALLOWED", message);
    }
}
