package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectArchiveNotAllowedException extends ApiException {

    public ProjectArchiveNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "PROJECT_ARCHIVE_NOT_ALLOWED", "Project cannot be archived because active dependencies or transactions still exist");
    }
}
