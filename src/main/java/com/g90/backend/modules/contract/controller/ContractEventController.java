package com.g90.backend.modules.contract.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.contract.dto.ContractEventContractListQuery;
import com.g90.backend.modules.contract.dto.ContractEventContractListResponseData;
import com.g90.backend.modules.contract.service.ContractEventService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contract-events")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ContractEventController {

    private final ContractEventService contractEventService;

    @GetMapping("/contracts")
    public ApiResponse<ContractEventContractListResponseData> getContractsByEventStatus(
            @Valid @ModelAttribute ContractEventContractListQuery query
    ) {
        return ApiResponse.success(
                "Contract event contract list fetched successfully",
                contractEventService.getContractsByEventStatus(query)
        );
    }
}
