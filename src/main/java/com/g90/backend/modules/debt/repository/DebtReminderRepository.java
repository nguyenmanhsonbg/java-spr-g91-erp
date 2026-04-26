package com.g90.backend.modules.debt.repository;

import com.g90.backend.modules.debt.entity.DebtReminderEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DebtReminderRepository extends JpaRepository<DebtReminderEntity, String> {

    List<DebtReminderEntity> findByCustomerIdOrderBySentAtDesc(String customerId);

    List<DebtReminderEntity> findAllByOrderBySentAtDesc();

    long countByInvoice_Id(String invoiceId);

    long countByInvoice_IdAndReminderTypeIn(String invoiceId, Collection<String> reminderTypes);

    List<DebtReminderEntity> findByInvoice_IdInOrderBySentAtDesc(Collection<String> invoiceIds);

    @Query("""
            select reminder.invoice.id
            from DebtReminderEntity reminder
            where reminder.invoice.id in :invoiceIds
              and reminder.reminderType = :reminderType
            """)
    List<String> findInvoiceIdsByReminderType(
            @Param("invoiceIds") Collection<String> invoiceIds,
            @Param("reminderType") String reminderType
    );
}
