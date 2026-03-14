package com.g90.backend.modules.quotation.repository;

import com.g90.backend.modules.quotation.entity.QuotationEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationRepository extends JpaRepository<QuotationEntity, String> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
