package com.g90.backend.modules.pricing.controller;

import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.pricing.dto.PriceListCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListDetailResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListItemUpdateRequest;
import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.dto.PriceListUpdateRequest;
import com.g90.backend.modules.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api")
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/price-lists")
    public ResponseEntity<ApiResponse<PriceListCreateDataResponse>> createPriceList(
            @Valid @RequestBody PriceListCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Price list created successfully", pricingService.createPriceList(request)));
    }

    @GetMapping("/price-lists")
    public ApiResponse<PriceListListResponseData> getPriceLists(@Valid @ModelAttribute PriceListListQuery query) {
        return ApiResponse.success("Price list fetched successfully", pricingService.getPriceLists(query));
    }

    @GetMapping("/price-lists/{id}")
    public ApiResponse<PriceListDetailResponse> getPriceListById(@PathVariable String id) {
        return ApiResponse.success("Price list detail fetched successfully", pricingService.getPriceListById(id));
    }

    @PutMapping("/price-lists/{id}")
    public ApiResponse<Void> updatePriceList(
            @PathVariable String id,
            @Valid @RequestBody PriceListUpdateRequest request
    ) {
        pricingService.updatePriceList(id, request);
        return ApiResponse.success("Price list updated successfully", null);
    }

    @DeleteMapping("/price-lists/{id}")
    public ApiResponse<Void> deletePriceList(@PathVariable String id) {
        pricingService.deletePriceList(id);
        return ApiResponse.success("Price list deleted successfully", null);
    }

    @PostMapping("/price-lists/{id}/items")
    public ResponseEntity<ApiResponse<PriceListItemCreateDataResponse>> addPriceListItem(
            @PathVariable String id,
            @Valid @RequestBody PriceListItemCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Price item added successfully", pricingService.addPriceListItem(id, request)));
    }

    @PutMapping("/price-list-items/{id}")
    public ApiResponse<Void> updatePriceListItem(
            @PathVariable String id,
            @Valid @RequestBody PriceListItemUpdateRequest request
    ) {
        pricingService.updatePriceListItem(id, request);
        return ApiResponse.success("Price item updated successfully", null);
    }

    @DeleteMapping("/price-list-items/{id}")
    public ApiResponse<Void> deletePriceListItem(@PathVariable String id) {
        pricingService.deletePriceListItem(id);
        return ApiResponse.success("Price item deleted successfully", null);
    }
}
