package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends ApiException {

    public FileStorageException(String code, String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, message, null);
    }

    public FileStorageException(String code, String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, message, null);
        initCause(cause);
    }
}
