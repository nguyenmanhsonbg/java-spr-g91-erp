package com.g90.backend.modules.debt.repository;

import com.g90.backend.modules.debt.entity.DebtReminderEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtReminderRepository extends JpaRepository<DebtReminderEntity, String> {

    List<DebtReminderEntity> findByCustomerIdOrderBySentAtDesc(String customerId);

    List<DebtReminderEntity> findAllByOrderBySentAtDesc();

    long countByInvoice_Id(String invoiceId);

    List<DebtReminderEntity> findByInvoice_IdInOrderBySentAtDesc(Collection<String> invoiceIds);
}
