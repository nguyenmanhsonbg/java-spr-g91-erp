package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class RegistrationVerificationCodeInvalidException extends ApiException {

    public RegistrationVerificationCodeInvalidException() {
        super(
                HttpStatus.BAD_REQUEST,
                "REGISTRATION_VERIFICATION_INVALID",
                "Verification code is invalid",
                List.of(ValidationErrorItem.builder()
                        .field("verificationCode")
                        .message("Verification code is invalid")
                        .build())
        );
    }
}
