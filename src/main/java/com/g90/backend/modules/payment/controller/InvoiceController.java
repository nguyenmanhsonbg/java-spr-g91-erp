package com.g90.backend.modules.payment.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.payment.dto.InvoiceCancelRequest;
import com.g90.backend.modules.payment.dto.InvoiceCreateRequest;
import com.g90.backend.modules.payment.dto.InvoiceListQuery;
import com.g90.backend.modules.payment.dto.InvoiceListResponseData;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.InvoiceUpdateRequest;
import com.g90.backend.modules.payment.service.InvoiceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@Valid @RequestBody InvoiceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", invoiceService.createInvoice(request)));
    }

    @GetMapping
    public ApiResponse<InvoiceListResponseData> getInvoices(@Valid @ModelAttribute InvoiceListQuery query) {
        return ApiResponse.success("Invoice list fetched successfully", invoiceService.getInvoices(query));
    }

    @GetMapping("/{invoiceId}")
    public ApiResponse<InvoiceResponse> getInvoice(@PathVariable String invoiceId) {
        return ApiResponse.success("Invoice detail fetched successfully", invoiceService.getInvoice(invoiceId));
    }

    @PutMapping("/{invoiceId}")
    public ApiResponse<InvoiceResponse> updateInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody InvoiceUpdateRequest request
    ) {
        return ApiResponse.success("Invoice updated successfully", invoiceService.updateInvoice(invoiceId, request));
    }

    @PostMapping("/{invoiceId}/cancel")
    public ApiResponse<InvoiceResponse> cancelInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody InvoiceCancelRequest request
    ) {
        return ApiResponse.success("Invoice cancelled successfully", invoiceService.cancelInvoice(invoiceId, request));
    }
}
