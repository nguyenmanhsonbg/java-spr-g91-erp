package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

public interface ContractPricingGateway {

    Map<String, PricingData> resolveBasePrices(CustomerProfileEntity customer, Collection<String> productIds);

    record PricingData(
            String productId,
            BigDecimal baseUnitPrice,
            String priceListId,
            String priceListName
    ) {
    }
}

@Component
class JpaContractPricingGateway implements ContractPricingGateway {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PriceListRepository priceListRepository;

    JpaContractPricingGateway(PriceListRepository priceListRepository) {
        this.priceListRepository = priceListRepository;
    }

    @Override
    public Map<String, PricingData> resolveBasePrices(CustomerProfileEntity customer, Collection<String> productIds) {
        Map<String, PricingData> pricingData = new LinkedHashMap<>();
        if (customer == null || !StringUtils.hasText(customer.getCustomerType()) || productIds == null || productIds.isEmpty()) {
            return pricingData;
        }

        List<PriceListEntity> priceLists = priceListRepository.findApplicablePriceLists(
                customer.getCustomerType().trim(),
                LocalDate.now(APP_ZONE)
        );
        if (priceLists.isEmpty()) {
            return pricingData;
        }

        PriceListEntity selected = priceLists.get(0);
        for (PriceListItemEntity item : selected.getItems()) {
            if (item.getProduct() == null || item.getUnitPrice() == null) {
                continue;
            }
            if (!productIds.contains(item.getProduct().getId())) {
                continue;
            }
            pricingData.putIfAbsent(
                    item.getProduct().getId(),
                    new PricingData(item.getProduct().getId(), item.getUnitPrice(), selected.getId(), selected.getName())
            );
        }
        return pricingData;
    }
}
