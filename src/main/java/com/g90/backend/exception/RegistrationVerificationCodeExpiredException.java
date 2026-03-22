package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class RegistrationVerificationCodeExpiredException extends ApiException {

    public RegistrationVerificationCodeExpiredException() {
        super(
                HttpStatus.BAD_REQUEST,
                "REGISTRATION_VERIFICATION_EXPIRED",
                "Verification code has expired",
                List.of(ValidationErrorItem.builder()
                        .field("verificationCode")
                        .message("Verification code has expired")
                        .build())
        );
    }
}
