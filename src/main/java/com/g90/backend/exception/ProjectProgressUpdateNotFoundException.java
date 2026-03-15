package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectProgressUpdateNotFoundException extends ApiException {

    public ProjectProgressUpdateNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROJECT_PROGRESS_UPDATE_NOT_FOUND", "Project progress update not found");
    }
}
