package com.g90.backend.modules.debt.repository;

import com.g90.backend.modules.debt.entity.PaymentAllocationEntity;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocationEntity, String> {

    List<PaymentAllocationEntity> findByInvoice_IdIn(Collection<String> invoiceIds);

    @EntityGraph(attributePaths = {"payment", "invoice"})
    @Query("""
            select pa
            from PaymentAllocationEntity pa
            where pa.invoice.id in :invoiceIds
            order by pa.payment.paymentDate desc, pa.payment.createdAt desc
            """)
    List<PaymentAllocationEntity> findDetailedByInvoiceIds(@Param("invoiceIds") Collection<String> invoiceIds);

    @Query("""
            select pa.invoice.id as invoiceId, coalesce(sum(pa.amount), 0) as allocatedAmount
            from PaymentAllocationEntity pa
            where pa.invoice.id in :invoiceIds
            group by pa.invoice.id
            """)
    List<InvoiceAllocationTotalView> summarizeByInvoiceIds(@Param("invoiceIds") Collection<String> invoiceIds);

    interface InvoiceAllocationTotalView {
        String getInvoiceId();

        BigDecimal getAllocatedAmount();
    }
}
