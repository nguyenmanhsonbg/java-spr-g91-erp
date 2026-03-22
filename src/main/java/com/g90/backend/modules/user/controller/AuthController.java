package com.g90.backend.modules.user.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.user.dto.ChangePasswordRequest;
import com.g90.backend.modules.user.dto.ForgotPasswordRequest;
import com.g90.backend.modules.user.dto.LoginRequest;
import com.g90.backend.modules.user.dto.LoginResponseData;
import com.g90.backend.modules.user.dto.RegistrationVerificationResponseData;
import com.g90.backend.modules.user.dto.RegisterRequest;
import com.g90.backend.modules.user.dto.RegisterResponseData;
import com.g90.backend.modules.user.dto.ResendVerificationCodeRequest;
import com.g90.backend.modules.user.dto.ResendVerificationCodeResponseData;
import com.g90.backend.modules.user.dto.ResetPasswordRequest;
import com.g90.backend.modules.user.dto.VerifyRegistrationRequest;
import com.g90.backend.modules.user.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserManagementService userManagementService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseData>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration created. Email verification required", userManagementService.register(request)));
    }

    @PostMapping("/verify-registration")
    public ApiResponse<RegistrationVerificationResponseData> verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        return ApiResponse.success("Registration verified successfully", userManagementService.verifyRegistration(request));
    }

    @PostMapping("/resend-verification-code")
    public ApiResponse<ResendVerificationCodeResponseData> resendVerificationCode(@Valid @RequestBody ResendVerificationCodeRequest request) {
        return ApiResponse.success("Verification code resent successfully", userManagementService.resendVerificationCode(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseData> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successfully", userManagementService.login(request));
    }

    @PostMapping("/logout")
    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME))
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        userManagementService.logout(authorizationHeader);
        return ApiResponse.success("Logout successfully", null);
    }

    @PostMapping("/change-password")
    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME))
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userManagementService.changePassword(request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userManagementService.forgotPassword(request);
        return ApiResponse.success("A confirmation email has been sent", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userManagementService.resetPassword(request);
        return ApiResponse.success("Password reset successfully", null);
    }
}
