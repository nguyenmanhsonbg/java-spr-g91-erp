package com.g90.backend.modules.pricing.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceListRepository extends JpaRepository<PriceListEntity, String>, JpaSpecificationExecutor<PriceListEntity> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select pl from PriceListEntity pl where pl.id = :id and pl.deletedAt is null")
    Optional<PriceListEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("""
            select distinct pl
            from PriceListEntity pl
            left join fetch pl.items items
            left join fetch items.product product
            where pl.deletedAt is null
              and upper(pl.customerGroup) = upper(:customerGroup)
              and upper(pl.status) = 'ACTIVE'
              and (pl.validFrom is null or pl.validFrom <= :today)
              and (pl.validTo is null or pl.validTo >= :today)
            order by pl.validFrom desc, pl.createdAt desc, pl.id desc
            """)
    List<PriceListEntity> findApplicablePriceLists(
            @Param("customerGroup") String customerGroup,
            @Param("today") LocalDate today
    );
}
