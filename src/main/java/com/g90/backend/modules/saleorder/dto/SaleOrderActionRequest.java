package com.g90.backend.modules.saleorder.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleOrderActionRequest {

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;

    @Size(max = 100, message = "trackingNumber must not exceed 100 characters")
    private String trackingNumber;

    private LocalDate actualDeliveryDate;
}
