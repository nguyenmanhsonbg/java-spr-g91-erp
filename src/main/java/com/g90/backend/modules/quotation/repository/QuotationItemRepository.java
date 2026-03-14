package com.g90.backend.modules.quotation.repository;

import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationItemRepository extends JpaRepository<QuotationItemEntity, String> {
}
