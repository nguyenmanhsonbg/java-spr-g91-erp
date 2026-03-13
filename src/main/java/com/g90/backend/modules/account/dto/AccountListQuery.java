package com.g90.backend.modules.account.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountListQuery {

    @Min(value = 0, message = "page must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than 0")
    private Integer size = 10;

    private String role;
    private String status;
}
