package com.g90.backend.exception;

import com.g90.backend.dto.ApiResponse;
import com.g90.backend.dto.ValidationErrorItem;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request data";
    private static final String REQUEST_FIELD = "request";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        return buildErrorResponse(exception.getStatus(), exception.getCode(), exception.getMessage(), exception.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return buildValidationResponse(extractFieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        return buildValidationResponse(extractFieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        List<ValidationErrorItem> errors = exception.getConstraintViolations().stream()
                .map(violation -> ValidationErrorItem.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .toList();

        return buildValidationResponse(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
        return buildValidationResponse(singleError(REQUEST_FIELD, "Request body is invalid"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(MissingServletRequestParameterException exception) {
        String field = exception.getParameterName();
        return buildValidationResponse(singleError(field, field + " is required"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPart(MissingServletRequestPartException exception) {
        String field = exception.getRequestPartName();
        return buildValidationResponse(singleError(field, field + " is required"));
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPathVariable(MissingPathVariableException exception) {
        String field = exception.getVariableName();
        return buildValidationResponse(singleError(field, field + " is required"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = hasText(exception.getName()) ? exception.getName() : REQUEST_FIELD;
        return buildValidationResponse(singleError(field, field + " has invalid value"));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(TypeMismatchException exception) {
        String field = hasText(exception.getPropertyName()) ? exception.getPropertyName() : REQUEST_FIELD;
        return buildValidationResponse(singleError(field, field + " has invalid value"));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiResponse<Void>> handleServletRequestBinding(ServletRequestBindingException exception) {
        String message = hasText(exception.getMessage()) ? exception.getMessage() : "Request binding is invalid";
        return buildValidationResponse(singleError(REQUEST_FIELD, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded() {
        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "PAYLOAD_TOO_LARGE",
                "Uploaded file is too large",
                singleError("files", "Uploaded file is too large")
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException() {
        return buildValidationResponse(singleError(REQUEST_FIELD, "Multipart request is invalid"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        String method = hasText(exception.getMethod()) ? exception.getMethod() : "HTTP method";
        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this endpoint",
                singleError("method", method + " is not supported for this endpoint")
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        String contentType = exception.getContentType() == null ? "Content type" : "Content type " + exception.getContentType();
        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Content type is not supported",
                singleError("contentType", contentType + " is not supported")
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotAcceptable() {
        return buildErrorResponse(
                HttpStatus.NOT_ACCEPTABLE,
                "NOT_ACCEPTABLE",
                "Requested media type is not acceptable",
                singleError("accept", "Requested media type is not acceptable")
        );
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleEndpointNotFound() {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "Endpoint not found");
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorResponseException(ErrorResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        return buildErrorResponse(status, resolveCode(status), resolveErrorResponseMessage(exception, status));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException() {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");
    }

    private List<ValidationErrorItem> extractFieldErrors(List<FieldError> fieldErrors) {
        Map<String, String> uniqueErrors = new LinkedHashMap<>();
        for (FieldError fieldError : fieldErrors) {
            uniqueErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        List<ValidationErrorItem> errors = new ArrayList<>();
        uniqueErrors.forEach((field, message) -> errors.add(ValidationErrorItem.builder()
                .field(field)
                .message(message)
                .build()));
        return errors;
    }

    private ResponseEntity<ApiResponse<Void>> buildValidationResponse(List<ValidationErrorItem> errors) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, INVALID_REQUEST_MESSAGE, errors);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatusCode status, String code, String message) {
        return buildErrorResponse(status, code, message, null);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatusCode status,
            String code,
            String message,
            List<ValidationErrorItem> errors
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(code, message, errors));
    }

    private List<ValidationErrorItem> singleError(String field, String message) {
        return List.of(ValidationErrorItem.builder()
                .field(field)
                .message(message)
                .build());
    }

    private String resolveErrorResponseMessage(ErrorResponseException exception, HttpStatusCode status) {
        if (hasText(exception.getBody().getDetail())) {
            return exception.getBody().getDetail();
        }
        return defaultMessage(status);
    }

    private String resolveCode(HttpStatusCode status) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        return httpStatus == null ? "HTTP_" + status.value() : httpStatus.name();
    }

    private String defaultMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> INVALID_REQUEST_MESSAGE;
            case 401 -> "Authentication required";
            case 403 -> "Permission denied";
            case 404 -> "Endpoint not found";
            case 405 -> "HTTP method is not supported for this endpoint";
            case 406 -> "Requested media type is not acceptable";
            case 415 -> "Content type is not supported";
            case 413 -> "Uploaded file is too large";
            default -> status.is5xxServerError() ? "An unexpected error occurred" : "Request cannot be processed";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
