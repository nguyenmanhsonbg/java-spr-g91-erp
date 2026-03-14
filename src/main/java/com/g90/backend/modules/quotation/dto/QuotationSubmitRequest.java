package com.g90.backend.modules.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotationSubmitRequest {

    @Size(max = 36, message = "Project ID must not exceed 36 characters")
    private String projectId;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    @Size(max = 1000, message = "Delivery requirement must not exceed 1000 characters")
    private String deliveryRequirement;

    @Valid
    @NotEmpty(message = "At least one quotation item is required")
    @Size(max = 20, message = "Quotation can contain at most 20 items")
    private List<QuotationItemRequest> items;
}
