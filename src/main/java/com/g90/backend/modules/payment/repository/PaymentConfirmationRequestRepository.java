package com.g90.backend.modules.payment.repository;

import com.g90.backend.modules.payment.entity.PaymentConfirmationRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentConfirmationRequestRepository extends JpaRepository<PaymentConfirmationRequestEntity, String> {

    boolean existsByInvoice_IdAndStatus(String invoiceId, String status);

    long countByStatus(String status);

    long countByCustomer_IdAndStatus(String customerId, String status);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer", "customer"})
    List<PaymentConfirmationRequestEntity> findByStatusOrderByCreatedAtAsc(String status);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer", "customer"})
    List<PaymentConfirmationRequestEntity> findByCustomer_IdAndStatusOrderByCreatedAtAsc(String customerId, String status);

    @EntityGraph(attributePaths = {"invoice", "customer"})
    @Query("""
            select r
            from PaymentConfirmationRequestEntity r
            order by r.createdAt desc
            """)
    List<PaymentConfirmationRequestEntity> findAllWithInvoiceAndCustomer();

    @EntityGraph(attributePaths = {"invoice", "customer"})
    @Query("""
            select r
            from PaymentConfirmationRequestEntity r
            where r.customer.id = :customerId
            order by r.createdAt desc
            """)
    List<PaymentConfirmationRequestEntity> findByCustomerIdWithInvoiceAndCustomer(@Param("customerId") String customerId);

    @EntityGraph(attributePaths = {"invoice", "customer"})
    @Query("""
            select r
            from PaymentConfirmationRequestEntity r
            where r.invoice.id = :invoiceId
            order by r.createdAt desc
            """)
    List<PaymentConfirmationRequestEntity> findByInvoiceIdWithInvoiceAndCustomer(@Param("invoiceId") String invoiceId);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer", "customer"})
    @Query("""
            select r
            from PaymentConfirmationRequestEntity r
            where r.id = :id
            """)
    Optional<PaymentConfirmationRequestEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer", "customer"})
    @Query("""
            select r
            from PaymentConfirmationRequestEntity r
            where r.id = :id
              and r.customer.id = :customerId
            """)
    Optional<PaymentConfirmationRequestEntity> findDetailedByIdAndCustomerId(@Param("id") String id, @Param("customerId") String customerId);
}
