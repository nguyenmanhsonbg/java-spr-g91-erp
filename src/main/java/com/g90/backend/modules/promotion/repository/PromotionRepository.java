package com.g90.backend.modules.promotion.repository;

import com.g90.backend.modules.promotion.entity.PromotionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<PromotionEntity, String> {

    @EntityGraph(attributePaths = {"products", "products.product"})
    @Query("""
            select distinct p
            from PromotionEntity p
            left join fetch p.products pp
            left join fetch pp.product product
            where upper(p.status) = 'ACTIVE'
              and p.code is not null
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
            order by p.startDate desc, p.name asc
            """)
    List<PromotionEntity> findActivePromotions(@Param("today") LocalDate today);

    @EntityGraph(attributePaths = {"products", "products.product"})
    @Query("""
            select distinct p
            from PromotionEntity p
            left join fetch p.products pp
            left join fetch pp.product product
            where upper(p.code) = upper(:code)
              and upper(p.status) = 'ACTIVE'
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
            """)
    Optional<PromotionEntity> findApplicableByCode(@Param("code") String code, @Param("today") LocalDate today);
}
