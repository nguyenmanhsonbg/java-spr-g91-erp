package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class SystemRoleNotConfiguredException extends ApiException {

    public SystemRoleNotConfiguredException(String roleName) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_NOT_CONFIGURED", "Required role is not configured: " + roleName);
    }
}
