package com.g90.backend.modules.debt.repository;

import com.g90.backend.modules.debt.entity.PaymentEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findByCustomerIdOrderByPaymentDateDescCreatedAtDesc(String customerId);

    @EntityGraph(attributePaths = {"allocations", "allocations.invoice"})
    Optional<PaymentEntity> findDetailedById(String id);

    @Query("""
            select count(p) > 0
            from DebtPaymentEntity p
            where p.customerId = :customerId
              and p.paymentDate = :paymentDate
              and p.amount = :amount
              and upper(coalesce(p.paymentMethod, '')) = upper(:paymentMethod)
              and (
                    (:referenceNo is null and p.referenceNo is null)
                    or upper(coalesce(p.referenceNo, '')) = upper(coalesce(:referenceNo, ''))
              )
            """)
    boolean existsDuplicate(
            @Param("customerId") String customerId,
            @Param("paymentDate") LocalDate paymentDate,
            @Param("amount") BigDecimal amount,
            @Param("paymentMethod") String paymentMethod,
            @Param("referenceNo") String referenceNo
    );
}
