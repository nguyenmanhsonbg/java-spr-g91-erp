package com.g90.backend.modules.pricing.repository;

import com.g90.backend.modules.pricing.entity.PriceListEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PriceListRepository extends JpaRepository<PriceListEntity, String>, JpaSpecificationExecutor<PriceListEntity> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<PriceListEntity> findWithItemsById(String id);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("""
            select distinct pl
            from PriceListEntity pl
            left join fetch pl.items items
            left join fetch items.product product
            where upper(pl.customerGroup) = upper(:customerGroup)
              and upper(pl.status) = 'ACTIVE'
              and (pl.startDate is null or pl.startDate <= :today)
              and (pl.endDate is null or pl.endDate >= :today)
            order by pl.startDate desc, pl.createdAt desc
            """)
    List<PriceListEntity> findApplicablePriceLists(
            @Param("customerGroup") String customerGroup,
            @Param("today") LocalDate today
    );
}
