package com.g90.backend.modules.contract.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementConfirmRequest;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementListResponseData;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementQuery;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementResponseData;
import com.g90.backend.modules.contract.service.ContractCancellationSettlementService;
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
@RequestMapping("/api/contract-cancellation-settlements")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ContractCancellationSettlementController {

    private final ContractCancellationSettlementService settlementService;

    @GetMapping
    public ApiResponse<ContractCancellationSettlementListResponseData> getSettlements(
            @Valid @ModelAttribute ContractCancellationSettlementQuery query
    ) {
        return ApiResponse.success(
                "Contract cancellation settlements fetched successfully",
                settlementService.getSettlements(query)
        );
    }

    @GetMapping("/{settlementId}")
    public ApiResponse<ContractCancellationSettlementResponseData> getSettlement(@PathVariable String settlementId) {
        return ApiResponse.success(
                "Contract cancellation settlement detail fetched successfully",
                settlementService.getSettlement(settlementId)
        );
    }

    @PostMapping("/{settlementId}/confirm-refund")
    public ApiResponse<ContractCancellationSettlementResponseData> confirmRefund(
            @PathVariable String settlementId,
            @Valid @RequestBody ContractCancellationSettlementConfirmRequest request
    ) {
        return ApiResponse.success(
                "Contract cancellation refund confirmed successfully",
                settlementService.confirmRefund(settlementId, request)
        );
    }
}
