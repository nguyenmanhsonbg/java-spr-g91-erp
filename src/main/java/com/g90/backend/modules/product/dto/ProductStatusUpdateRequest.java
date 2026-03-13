package com.g90.backend.modules.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusUpdateRequest {

    private String status;
    private String reason;
    private String requestedByRole;
}
