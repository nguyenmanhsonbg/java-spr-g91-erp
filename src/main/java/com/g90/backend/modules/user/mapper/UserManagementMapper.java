package com.g90.backend.modules.user.mapper;

import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.user.dto.AuthenticatedUserResponse;
import com.g90.backend.modules.user.dto.LoginResponseData;
import com.g90.backend.modules.user.dto.RegistrationVerificationResponseData;
import com.g90.backend.modules.user.dto.RegisterResponseData;
import com.g90.backend.modules.user.dto.ResendVerificationCodeResponseData;
import com.g90.backend.modules.user.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserManagementMapper {

    public RegisterResponseData toRegisterData(UserAccountEntity entity, int expireMinutes) {
        return RegisterResponseData.builder()
                .userId(entity.getId())
                .email(entity.getEmail())
                .verificationRequired(true)
                .expireMinutes(expireMinutes)
                .redirectTo("/verify-registration")
                .build();
    }

    public RegistrationVerificationResponseData toRegistrationVerificationData(UserAccountEntity entity) {
        return RegistrationVerificationResponseData.builder()
                .userId(entity.getId())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .verified(Boolean.TRUE.equals(entity.getEmailVerified()))
                .redirectTo("/login")
                .build();
    }

    public ResendVerificationCodeResponseData toResendVerificationCodeData(UserAccountEntity entity, int expireMinutes) {
        return ResendVerificationCodeResponseData.builder()
                .userId(entity.getId())
                .email(entity.getEmail())
                .expireMinutes(expireMinutes)
                .build();
    }

    public LoginResponseData toLoginData(String accessToken, UserAccountEntity entity) {
        return LoginResponseData.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(toAuthenticatedUser(entity))
                .build();
    }

    public AuthenticatedUserResponse toAuthenticatedUser(UserAccountEntity entity) {
        return AuthenticatedUserResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole().getName())
                .status(entity.getStatus())
                .build();
    }

    public UserProfileResponse toProfile(UserAccountEntity entity) {
        return UserProfileResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole().getName())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
