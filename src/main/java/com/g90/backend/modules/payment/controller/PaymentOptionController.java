package com.g90.backend.modules.payment.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import com.g90.backend.modules.payment.service.PaymentOptionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-options")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class PaymentOptionController {

    private final PaymentOptionService paymentOptionService;

    @GetMapping
    public ApiResponse<List<PaymentOptionData>> getPaymentOptions() {
        return ApiResponse.success("Payment options fetched successfully", paymentOptionService.getPaymentOptions());
    }
}
