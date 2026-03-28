package com.g90.backend.modules.promotion.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.promotion.dto.PromotionCreateDataResponse;
import com.g90.backend.modules.promotion.dto.PromotionCreateRequest;
import com.g90.backend.modules.promotion.dto.PromotionDetailResponse;
import com.g90.backend.modules.promotion.dto.PromotionListQuery;
import com.g90.backend.modules.promotion.dto.PromotionListResponseData;
import com.g90.backend.modules.promotion.dto.PromotionUpdateRequest;
import com.g90.backend.modules.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<PromotionCreateDataResponse>> createPromotion(
            @Valid @RequestBody PromotionCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion created successfully", promotionService.createPromotion(request)));
    }

    @GetMapping("/promotions")
    public ApiResponse<PromotionListResponseData> getPromotions(@Valid @ModelAttribute PromotionListQuery query) {
        return ApiResponse.success("Promotions fetched successfully", promotionService.getPromotions(query));
    }

    @GetMapping("/promotions/{id}")
    public ApiResponse<PromotionDetailResponse> getPromotionById(@PathVariable String id) {
        return ApiResponse.success("Promotion fetched successfully", promotionService.getPromotionById(id));
    }

    @PutMapping("/promotions/{id}")
    public ApiResponse<PromotionDetailResponse> updatePromotion(
            @PathVariable String id,
            @Valid @RequestBody PromotionUpdateRequest request
    ) {
        return ApiResponse.success("Promotion updated successfully", promotionService.updatePromotion(id, request));
    }

    @DeleteMapping("/promotions/{id}")
    public ApiResponse<Void> deletePromotion(@PathVariable String id) {
        promotionService.deletePromotion(id);
        return ApiResponse.success("Promotion deleted successfully", null);
    }
}
