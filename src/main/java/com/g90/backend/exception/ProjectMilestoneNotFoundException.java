package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectMilestoneNotFoundException extends ApiException {

    public ProjectMilestoneNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROJECT_MILESTONE_NOT_FOUND", "Project milestone not found");
    }
}
