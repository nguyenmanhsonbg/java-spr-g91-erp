package com.g90.backend.modules.inventory.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.inventory.dto.InventoryAdjustmentRequest;
import com.g90.backend.modules.inventory.dto.InventoryHistoryQuery;
import com.g90.backend.modules.inventory.dto.InventoryHistoryResponseData;
import com.g90.backend.modules.inventory.dto.InventoryIssueRequest;
import com.g90.backend.modules.inventory.dto.InventoryMutationResponse;
import com.g90.backend.modules.inventory.dto.InventoryReceiptRequest;
import com.g90.backend.modules.inventory.dto.InventoryStatusQuery;
import com.g90.backend.modules.inventory.dto.InventoryStatusResponseData;
import com.g90.backend.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/receipts")
    public ResponseEntity<ApiResponse<InventoryMutationResponse>> createReceipt(
            @Valid @RequestBody InventoryReceiptRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory receipt created successfully", inventoryService.createReceipt(request)));
    }

    @PostMapping("/issues")
    public ResponseEntity<ApiResponse<InventoryMutationResponse>> createIssue(
            @Valid @RequestBody InventoryIssueRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory issue created successfully", inventoryService.createIssue(request)));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<ApiResponse<InventoryMutationResponse>> adjustInventory(
            @Valid @RequestBody InventoryAdjustmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory adjustment created successfully", inventoryService.adjustInventory(request)));
    }

    @GetMapping("/status")
    public ApiResponse<InventoryStatusResponseData> getInventoryStatus(@Valid @ModelAttribute InventoryStatusQuery query) {
        return ApiResponse.success("Inventory status fetched successfully", inventoryService.getInventoryStatus(query));
    }

    @GetMapping("/history")
    public ApiResponse<InventoryHistoryResponseData> getInventoryHistory(@Valid @ModelAttribute InventoryHistoryQuery query) {
        return ApiResponse.success("Inventory history fetched successfully", inventoryService.getInventoryHistory(query));
    }
}
