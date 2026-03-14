package com.g90.backend.modules.pricing.repository;

import com.g90.backend.modules.pricing.entity.PriceListEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PriceListRepository extends JpaRepository<PriceListEntity, String>, JpaSpecificationExecutor<PriceListEntity> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<PriceListEntity> findWithItemsById(String id);
}
