package com.g90.backend.modules.debt.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.debt.dto.OpenInvoiceResponse;
import com.g90.backend.modules.debt.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class CustomerDebtController {

    private final PaymentService paymentService;

    @GetMapping("/{customerId}/open-invoices")
    public ApiResponse<List<OpenInvoiceResponse>> getOpenInvoices(@PathVariable String customerId) {
        return ApiResponse.success("Open invoices fetched successfully", paymentService.getOpenInvoices(customerId));
    }
}
