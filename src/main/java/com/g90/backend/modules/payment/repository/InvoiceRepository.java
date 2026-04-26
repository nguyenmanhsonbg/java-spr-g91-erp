package com.g90.backend.modules.payment.repository;

import com.g90.backend.modules.payment.entity.InvoiceEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByIssueDateBetween(LocalDate start, LocalDate end);

    boolean existsByContract_IdAndStatusNotIn(String contractId, Collection<String> statuses);

    @EntityGraph(attributePaths = {"customer", "customer.user", "contract"})
    @Query("select i from PaymentInvoiceEntity i")
    List<InvoiceEntity> findAllWithCustomerAndContract();

    @EntityGraph(attributePaths = {"customer", "customer.user", "contract"})
    @Query("select i from PaymentInvoiceEntity i where i.customer.id = :customerId")
    List<InvoiceEntity> findByCustomerIdWithCustomerAndContract(@Param("customerId") String customerId);

    @EntityGraph(attributePaths = {"customer", "customer.user", "contract"})
    @Query("select i from PaymentInvoiceEntity i where i.contract.id = :contractId order by i.issueDate desc, i.createdAt desc")
    List<InvoiceEntity> findByContractIdWithCustomerAndContract(@Param("contractId") String contractId);

    @Query("""
            select i
            from PaymentInvoiceEntity i
            left join fetch i.customer customer
            left join fetch customer.user
            left join fetch i.contract contract
            where i.dueDate between :fromDate and :toDate
              and (i.status is null or upper(i.status) not in :excludedStatuses)
            order by customer.id asc, i.dueDate asc, i.createdAt asc
            """)
    List<InvoiceEntity> findDueReminderCandidates(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("excludedStatuses") Collection<String> excludedStatuses
    );

    @Query("""
            select distinct i
            from PaymentInvoiceEntity i
            left join fetch i.customer customer
            left join fetch customer.user
            left join fetch i.contract contract
            left join fetch i.items item
            left join fetch item.product
            where i.id = :id
            """)
    Optional<InvoiceEntity> findDetailedById(@Param("id") String id);

    @Query("""
            select distinct i
            from PaymentInvoiceEntity i
            left join fetch i.customer customer
            left join fetch customer.user
            left join fetch i.contract contract
            left join fetch i.items item
            left join fetch item.product
            where i.id = :id
              and customer.id = :customerId
            """)
    Optional<InvoiceEntity> findDetailedByIdAndCustomerId(@Param("id") String id, @Param("customerId") String customerId);
}
