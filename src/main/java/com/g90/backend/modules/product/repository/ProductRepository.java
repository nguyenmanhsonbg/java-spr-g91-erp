package com.g90.backend.modules.product.repository;

import com.g90.backend.modules.product.entity.ProductEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, String>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByProductCodeIgnoreCase(String productCode);

    @EntityGraph(attributePaths = "images")
    @Query("select product from ProductEntity product where product.id = :id")
    Optional<ProductEntity> findDetailedById(@Param("id") String id);
}
