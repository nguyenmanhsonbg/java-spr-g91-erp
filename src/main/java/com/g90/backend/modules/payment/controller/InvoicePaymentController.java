package com.g90.backend.modules.payment.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestCreateRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListQuery;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListResponseData;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestResponse;
import com.g90.backend.modules.payment.dto.PaymentInstructionResponse;
import com.g90.backend.modules.payment.service.PaymentConfirmationRequestService;
import com.g90.backend.modules.payment.service.PaymentInstructionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class InvoicePaymentController {

    private final PaymentInstructionService paymentInstructionService;
    private final PaymentConfirmationRequestService paymentConfirmationRequestService;

    @GetMapping("/{invoiceId}/payment-instruction")
    public ApiResponse<PaymentInstructionResponse> getPaymentInstruction(@PathVariable String invoiceId) {
        return ApiResponse.success(
                "Payment instruction fetched successfully",
                paymentInstructionService.getPaymentInstruction(invoiceId)
        );
    }

    @PostMapping("/{invoiceId}/payment-confirmation-requests")
    public ResponseEntity<ApiResponse<PaymentConfirmationRequestResponse>> createPaymentConfirmationRequest(
            @PathVariable String invoiceId,
            @Valid @RequestBody PaymentConfirmationRequestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Payment confirmation request created successfully",
                        paymentConfirmationRequestService.createRequest(invoiceId, request)
                ));
    }

    @GetMapping("/{invoiceId}/payment-confirmation-requests")
    public ApiResponse<PaymentConfirmationRequestListResponseData> getPaymentConfirmationRequestsByInvoice(
            @PathVariable String invoiceId,
            @Valid @ModelAttribute PaymentConfirmationRequestListQuery query
    ) {
        return ApiResponse.success(
                "Payment confirmation request list fetched successfully",
                paymentConfirmationRequestService.listRequestsByInvoice(invoiceId, query)
        );
    }
}
