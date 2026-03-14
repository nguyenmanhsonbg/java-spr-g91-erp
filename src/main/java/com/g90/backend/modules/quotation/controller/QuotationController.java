package com.g90.backend.modules.quotation.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.quotation.dto.QuotationDetailResponseData;
import com.g90.backend.modules.quotation.dto.QuotationHistoryResponseData;
import com.g90.backend.modules.quotation.dto.QuotationPreviewByIdResponseData;
import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSaveResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitActionRequest;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;
import com.g90.backend.modules.quotation.dto.QuotationSubmitResponseData;
import com.g90.backend.modules.quotation.service.QuotationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        return ApiResponse.success("Quotation preview calculated successfully", quotationService.previewQuotation(request));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuotationSubmitResponseData>> createQuotation(@Valid @RequestBody QuotationSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation created successfully", quotationService.createQuotation(request)));
    }

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<QuotationSaveResponseData>> saveDraftQuotation(@Valid @RequestBody QuotationSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation draft saved successfully", quotationService.saveDraftQuotation(request)));
    }

    @PostMapping("/submit")
    public ApiResponse<QuotationSubmitResponseData> submitQuotation(@Valid @RequestBody QuotationSubmitActionRequest request) {
        return ApiResponse.success("Quotation submitted successfully", quotationService.submitQuotation(request));
    }

    @GetMapping("/{quotationId}/preview")
    public ApiResponse<QuotationPreviewByIdResponseData> getQuotationPreview(@PathVariable String quotationId) {
        return ApiResponse.success("Quotation preview loaded successfully", quotationService.getQuotationPreview(quotationId));
    }

    @PutMapping("/{quotationId}")
    public ApiResponse<QuotationSaveResponseData> updateDraftQuotation(
            @PathVariable String quotationId,
            @Valid @RequestBody QuotationSubmitRequest request
    ) {
        return ApiResponse.success("Quotation updated successfully", quotationService.updateDraftQuotation(quotationId, request));
    }

    @PostMapping("/{quotationId}/submit")
    public ApiResponse<QuotationSubmitResponseData> submitDraftQuotation(@PathVariable String quotationId) {
        return ApiResponse.success("Quotation submitted successfully", quotationService.submitQuotation(quotationId));
    }

    @GetMapping("/{quotationId}")
    public ApiResponse<QuotationDetailResponseData> getQuotationDetail(@PathVariable String quotationId) {
        return ApiResponse.success("Quotation detail fetched successfully", quotationService.getQuotationDetail(quotationId));
    }

    @GetMapping("/{quotationId}/history")
    public ApiResponse<QuotationHistoryResponseData> getQuotationHistory(@PathVariable String quotationId) {
        return ApiResponse.success("Quotation history fetched successfully", quotationService.getQuotationHistory(quotationId));
    }
}
