package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectRestoreNotAllowedException extends ApiException {

    public ProjectRestoreNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "PROJECT_RESTORE_NOT_ALLOWED", "Project can no longer be restored");
    }
}
