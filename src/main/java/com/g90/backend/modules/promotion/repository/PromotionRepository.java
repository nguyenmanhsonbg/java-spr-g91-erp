package com.g90.backend.modules.promotion.repository;

import com.g90.backend.modules.promotion.entity.PromotionEntity;
import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<PromotionEntity, String>, JpaSpecificationExecutor<PromotionEntity> {

    boolean existsByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = {"products", "products.product", "customerGroups"})
    @Query("select distinct p from PromotionEntity p where p.id = :id and p.deletedAt is null")
    Optional<PromotionEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {"products", "products.product", "customerGroups"})
    @Query("""
            select distinct p
            from PromotionEntity p
            left join fetch p.products pp
            left join fetch pp.product product
            left join fetch p.customerGroups customerGroup
            where p.deletedAt is null
              and upper(p.status) = 'ACTIVE'
              and p.code is not null
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
            order by coalesce(p.priority, 0) desc, p.updatedAt desc, p.createdAt desc, p.name asc
            """)
    List<PromotionEntity> findActivePromotions(@Param("today") LocalDate today);

    @EntityGraph(attributePaths = {"products", "products.product", "customerGroups"})
    @Query("""
            select distinct p
            from PromotionEntity p
            left join fetch p.products pp
            left join fetch pp.product product
            left join fetch p.customerGroups customerGroup
            where p.deletedAt is null
              and upper(p.status) = 'ACTIVE'
              and p.code is not null
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
              and (
                    not exists (
                        select 1
                        from PromotionCustomerGroupEntity groupScope
                        where groupScope.promotion = p
                          and groupScope.deletedAt is null
                    )
                    or exists (
                        select 1
                        from PromotionCustomerGroupEntity groupScope
                        where groupScope.promotion = p
                          and groupScope.deletedAt is null
                          and upper(groupScope.customerGroup) in :customerGroups
                    )
                  )
            order by coalesce(p.priority, 0) desc, p.updatedAt desc, p.createdAt desc, p.name asc
            """)
    List<PromotionEntity> findVisibleActivePromotions(
            @Param("today") LocalDate today,
            @Param("customerGroups") Collection<String> customerGroups
    );

    @EntityGraph(attributePaths = {"products", "products.product", "customerGroups"})
    @Query("""
            select distinct p
            from PromotionEntity p
            left join fetch p.products pp
            left join fetch pp.product product
            left join fetch p.customerGroups customerGroup
            where upper(p.code) = upper(:code)
              and p.deletedAt is null
              and upper(p.status) = 'ACTIVE'
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
            """)
    Optional<PromotionEntity> findApplicableByCode(@Param("code") String code, @Param("today") LocalDate today);
}
