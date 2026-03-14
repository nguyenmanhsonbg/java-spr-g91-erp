package com.g90.backend.modules.contract.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.contract.dto.ContractFromQuotationResponseData;
import com.g90.backend.modules.contract.dto.CreateContractFromQuotationRequest;
import com.g90.backend.modules.contract.service.ContractService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/from-quotation/{quotationId}")
    public ApiResponse<ContractFromQuotationResponseData> createFromQuotation(
            @PathVariable String quotationId,
            @Valid @RequestBody CreateContractFromQuotationRequest request
    ) {
        return ApiResponse.success(
                "Contract created from quotation successfully",
                contractService.createFromQuotation(quotationId, request)
        );
    }
}
