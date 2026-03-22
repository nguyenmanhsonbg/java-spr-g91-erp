package com.g90.backend.modules.user.service;

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
import com.g90.backend.modules.user.dto.UpdateProfileRequest;
import com.g90.backend.modules.user.dto.UserProfileResponse;
import com.g90.backend.modules.user.dto.VerifyRegistrationRequest;

public interface UserManagementService {

    RegisterResponseData register(RegisterRequest request);

    RegistrationVerificationResponseData verifyRegistration(VerifyRegistrationRequest request);

    ResendVerificationCodeResponseData resendVerificationCode(ResendVerificationCodeRequest request);

    LoginResponseData login(LoginRequest request);

    void logout(String authorizationHeader);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserProfileResponse getMyProfile();

    UserProfileResponse updateMyProfile(UpdateProfileRequest request);
}
