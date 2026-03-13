package com.g90.backend.modules.product.repository;

import com.g90.backend.modules.product.entity.ProductEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<ProductEntity, String>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByProductCodeIgnoreCase(String productCode);
}
