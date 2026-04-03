package com.g90.backend.modules.debt.repository;

import com.g90.backend.modules.debt.entity.DebtInvoiceEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtInvoiceRepository extends JpaRepository<DebtInvoiceEntity, String> {

    List<DebtInvoiceEntity> findByCustomerIdOrderByDueDateAscCreatedAtAsc(String customerId);

    List<DebtInvoiceEntity> findByIdIn(Collection<String> ids);

    Optional<DebtInvoiceEntity> findByIdAndCustomerId(String id, String customerId);
}
