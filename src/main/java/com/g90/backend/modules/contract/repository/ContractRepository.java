package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<ContractEntity, String>, JpaSpecificationExecutor<ContractEntity> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countBySubmittedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByQuotation_Id(String quotationId);

    boolean existsByPriceListIdAndStatusIn(String priceListId, Collection<String> statuses);

    boolean existsByContractNumberIgnoreCase(String contractNumber);

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "quotation",
            "paymentOption",
            "items",
            "items.product"
    })
    @Query("select distinct c from ContractEntity c where c.id = :id")
    Optional<ContractEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "quotation",
            "paymentOption",
            "items",
            "items.product"
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

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
            select distinct c
            from ContractEntity c
            left join fetch c.customer customer
            where c.status in :statuses
              and (c.saleOrderNumber is not null or c.submittedAt is not null)
            order by c.expectedDeliveryDate asc, c.submittedAt asc, c.createdAt asc
            """)
    List<ContractEntity> findSaleOrdersByStatusIn(@Param("statuses") Collection<String> statuses);

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
            select distinct c
            from ContractEntity c
            left join fetch c.customer customer
            where c.customer.id = :customerId
              and c.status in :statuses
              and (c.saleOrderNumber is not null or c.submittedAt is not null)
            order by c.expectedDeliveryDate asc, c.submittedAt asc, c.createdAt asc
            """)
    List<ContractEntity> findSaleOrdersByCustomerIdAndStatusIn(
            @Param("customerId") String customerId,
            @Param("statuses") Collection<String> statuses
    );
}
