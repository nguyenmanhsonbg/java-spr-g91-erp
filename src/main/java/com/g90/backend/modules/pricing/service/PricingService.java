package com.g90.backend.modules.pricing.service;

import com.g90.backend.modules.pricing.dto.PriceListCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListDetailResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateDataResponse;
import com.g90.backend.modules.pricing.dto.PriceListItemCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListItemUpdateRequest;
import com.g90.backend.modules.pricing.dto.PriceListListQuery;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.dto.PriceListUpdateRequest;

public interface PricingService {

    PriceListCreateDataResponse createPriceList(PriceListCreateRequest request);

    PriceListListResponseData getPriceLists(PriceListListQuery query);

    PriceListDetailResponse getPriceListById(String id);

    void updatePriceList(String id, PriceListUpdateRequest request);

    void deletePriceList(String id);

    PriceListItemCreateDataResponse addPriceListItem(String priceListId, PriceListItemCreateRequest request);

    void updatePriceListItem(String itemId, PriceListItemUpdateRequest request);

    void deletePriceListItem(String itemId);
}
