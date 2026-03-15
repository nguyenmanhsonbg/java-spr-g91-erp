package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectNotFoundException extends ApiException {

    public ProjectNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found");
    }
}
