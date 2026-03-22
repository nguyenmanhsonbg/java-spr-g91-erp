package com.g90.backend.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Size(min = 5, max = 5, message = "Verification code must be exactly 5 characters")
    @Pattern(regexp = "^[A-Z0-9]{5}$", message = "Verification code must contain only uppercase letters and digits")
    private String verificationCode;
}
