package com.g90.backend.modules.pricing.repository;

import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceListItemRepository extends JpaRepository<PriceListItemEntity, String> {

    List<PriceListItemEntity> findByPriceList_Id(String priceListId);
}
