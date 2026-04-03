package com.g90.backend.modules.debt.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.debt.dto.PaymentCreateRequest;
import com.g90.backend.modules.debt.dto.PaymentResponse;
import com.g90.backend.modules.debt.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(@Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", paymentService.recordPayment(request)));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ApiResponse.success("Payment detail fetched successfully", paymentService.getPayment(paymentId));
    }
}
