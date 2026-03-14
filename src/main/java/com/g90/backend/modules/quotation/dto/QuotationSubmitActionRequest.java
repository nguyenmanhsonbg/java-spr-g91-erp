package com.g90.backend.modules.quotation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotationSubmitActionRequest {

    @Size(max = 36, message = "Quotation ID must not exceed 36 characters")
    private String quotationId;

    @Size(max = 36, message = "Project ID must not exceed 36 characters")
    private String projectId;

    @JsonProperty("deliveryRequirements")
    @JsonAlias("deliveryRequirement")
    @Size(max = 1000, message = "Delivery requirement must not exceed 1000 characters")
    private String deliveryRequirements;

    @Size(max = 50, message = "Promotion code must not exceed 50 characters")
    private String promotionCode;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    @Valid
    private List<QuotationItemRequest> items;
}
