package com.g90.backend.modules.inventory.repository;

import com.g90.backend.modules.inventory.entity.InventoryStockEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface InventoryStockRepository extends JpaRepository<InventoryStockEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select stock
            from InventoryStockEntity stock
            left join fetch stock.warehouse warehouse
            where stock.product.id = :productId
            order by stock.updatedAt asc, stock.id asc
            """)
    List<InventoryStockEntity> lockByProductId(@Param("productId") String productId);

    @Query("""
            select stock.product.id as productId,
                   coalesce(sum(stock.quantity), 0) as quantity,
                   max(stock.updatedAt) as updatedAt
            from InventoryStockEntity stock
            where stock.product.id in :productIds
            group by stock.product.id
            """)
    List<InventoryStockSummaryView> summarizeByProductIds(@Param("productIds") Collection<String> productIds);

    interface InventoryStockSummaryView {
        String getProductId();

        BigDecimal getQuantity();

        LocalDateTime getUpdatedAt();
    }
}
