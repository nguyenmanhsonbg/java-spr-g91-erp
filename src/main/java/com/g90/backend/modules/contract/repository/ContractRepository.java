package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<ContractEntity, String>, JpaSpecificationExecutor<ContractEntity> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByQuotation_Id(String quotationId);

    boolean existsByContractNumberIgnoreCase(String contractNumber);

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "quotation",
            "items",
            "items.product",
            "versions",
            "approvals",
            "statusHistory",
            "documents",
            "trackingEvents"
    })
    @Query("select distinct c from ContractEntity c where c.id = :id")
    Optional<ContractEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "quotation",
            "items",
            "items.product",
            "versions",
            "approvals",
            "statusHistory",
            "documents",
            "trackingEvents"
    })
    @Query("select distinct c from ContractEntity c where c.id = :id and c.customer.id = :customerId")
    Optional<ContractEntity> findDetailedByIdAndCustomer_Id(@Param("id") String id, @Param("customerId") String customerId);

    @Query("""
            select distinct c
            from ContractEntity c
            left join fetch c.customer customer
            left join fetch customer.user customerUser
            where upper(c.approvalStatus) = 'PENDING'
            order by c.approvalRequestedAt asc, c.createdAt asc
            """)
    List<ContractEntity> findPendingApprovalContracts();
}
