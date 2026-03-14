package com.g90.backend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Size(max = 255, message = "Current password must not exceed 255 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    @Size(max = 255, message = "New password must not exceed 255 characters")
    private String newPassword;

    @NotBlank(message = "Confirm new password is required")
    @Size(min = 6, message = "Confirm new password must be at least 6 characters")
    @Size(max = 255, message = "Confirm new password must not exceed 255 characters")
    private String confirmNewPassword;
}
