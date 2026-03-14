package com.g90.backend.modules.quotation.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;
import com.g90.backend.modules.quotation.service.QuotationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quotations")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping("/preview")
    public ApiResponse<QuotationPreviewResponseData> previewQuotation(@Valid @RequestBody QuotationSubmitRequest request) {
        return ApiResponse.success("Quotation preview generated successfully", quotationService.previewQuotation(request));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuotationResponseData>> createQuotation(@Valid @RequestBody QuotationSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation created successfully", quotationService.createQuotation(request)));
    }

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<QuotationResponseData>> saveDraftQuotation(@Valid @RequestBody QuotationSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation draft saved successfully", quotationService.saveDraftQuotation(request)));
    }
}
