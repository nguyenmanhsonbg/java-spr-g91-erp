package com.g90.backend.modules.promotion.service;

import com.g90.backend.modules.promotion.dto.PromotionCreateDataResponse;
import com.g90.backend.modules.promotion.dto.PromotionCreateRequest;
import com.g90.backend.modules.promotion.dto.PromotionDetailResponse;
import com.g90.backend.modules.promotion.dto.PromotionListQuery;
import com.g90.backend.modules.promotion.dto.PromotionListResponseData;
import com.g90.backend.modules.promotion.dto.PromotionUpdateRequest;

public interface PromotionService {

    PromotionCreateDataResponse createPromotion(PromotionCreateRequest request);

    PromotionListResponseData getPromotions(PromotionListQuery query);

    PromotionDetailResponse getPromotionById(String id);

    PromotionDetailResponse updatePromotion(String id, PromotionUpdateRequest request);

    void deletePromotion(String id);
}
