package com.g90.backend.modules.payment.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestConfirmRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListQuery;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListResponseData;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestRejectRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestResponse;
import com.g90.backend.modules.payment.service.PaymentConfirmationRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/payment-confirmation-requests")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class PaymentConfirmationRequestController {

    private final PaymentConfirmationRequestService paymentConfirmationRequestService;

    @GetMapping
    public ApiResponse<PaymentConfirmationRequestListResponseData> getPaymentConfirmationRequests(
            @Valid @ModelAttribute PaymentConfirmationRequestListQuery query
    ) {
        return ApiResponse.success(
                "Payment confirmation request list fetched successfully",
                paymentConfirmationRequestService.listRequests(query)
        );
    }

    @GetMapping("/{requestId}")
    public ApiResponse<PaymentConfirmationRequestResponse> getPaymentConfirmationRequest(@PathVariable String requestId) {
        return ApiResponse.success(
                "Payment confirmation request detail fetched successfully",
                paymentConfirmationRequestService.getDetail(requestId)
        );
    }

    @PostMapping("/{requestId}/confirm")
    public ApiResponse<PaymentConfirmationRequestResponse> confirmPaymentConfirmationRequest(
            @PathVariable String requestId,
            @Valid @RequestBody PaymentConfirmationRequestConfirmRequest request
    ) {
        return ApiResponse.success(
                "Payment confirmation request confirmed successfully",
                paymentConfirmationRequestService.confirmRequest(requestId, request)
        );
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<PaymentConfirmationRequestResponse> rejectPaymentConfirmationRequest(
            @PathVariable String requestId,
            @Valid @RequestBody PaymentConfirmationRequestRejectRequest request
    ) {
        return ApiResponse.success(
                "Payment confirmation request rejected successfully",
                paymentConfirmationRequestService.rejectRequest(requestId, request)
        );
    }
}
