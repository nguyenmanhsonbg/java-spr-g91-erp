package com.g90.backend.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;
    private String address;

    @NotBlank(message = "Role is required")
    private String roleId;

    @NotBlank(message = "Status is required")
    private String status;
}
