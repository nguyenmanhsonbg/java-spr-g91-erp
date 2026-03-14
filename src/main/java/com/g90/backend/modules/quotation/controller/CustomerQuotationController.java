package com.g90.backend.modules.quotation.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.quotation.dto.CustomerQuotationListQuery;
import com.g90.backend.modules.quotation.dto.CustomerQuotationListResponseData;
import com.g90.backend.modules.quotation.dto.CustomerQuotationSummaryResponseData;
import com.g90.backend.modules.quotation.dto.QuotationFormInitQuery;
import com.g90.backend.modules.quotation.dto.QuotationFormInitResponseData;
import com.g90.backend.modules.quotation.service.QuotationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class CustomerQuotationController {

    private final QuotationService quotationService;

    @GetMapping("/quotation-form-init")
    public ApiResponse<QuotationFormInitResponseData> getQuotationFormInit(@Valid @ModelAttribute QuotationFormInitQuery query) {
        return ApiResponse.success("Quotation form data loaded successfully", quotationService.getQuotationFormInit(query));
    }

    @GetMapping("/quotations")
    public ApiResponse<CustomerQuotationListResponseData> getMyQuotations(@Valid @ModelAttribute CustomerQuotationListQuery query) {
        return ApiResponse.success("Quotation list fetched successfully", quotationService.getMyQuotations(query));
    }

    @GetMapping("/quotations/summary")
    public ApiResponse<CustomerQuotationSummaryResponseData> getMyQuotationSummary() {
        return ApiResponse.success("Quotation summary fetched successfully", quotationService.getMyQuotationSummary());
    }
}
