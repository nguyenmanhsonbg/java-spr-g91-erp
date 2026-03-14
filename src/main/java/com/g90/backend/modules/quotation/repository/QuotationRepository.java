package com.g90.backend.modules.quotation.repository;

import com.g90.backend.modules.quotation.entity.QuotationEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationRepository extends JpaRepository<QuotationEntity, String>, JpaSpecificationExecutor<QuotationEntity> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByCustomer_Id(String customerId);

    long countByCustomer_IdAndStatusIgnoreCase(String customerId, String status);

    @EntityGraph(attributePaths = {"customer", "customer.user", "project", "items", "items.product"})
    @Query("select q from QuotationEntity q where q.id = :id")
    Optional<QuotationEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {"customer", "customer.user", "project", "items", "items.product"})
    @Query("select q from QuotationEntity q where q.id = :id and q.customer.id = :customerId")
    Optional<QuotationEntity> findDetailedByIdAndCustomer_Id(@Param("id") String id, @Param("customerId") String customerId);
}
