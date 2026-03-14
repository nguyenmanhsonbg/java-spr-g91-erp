package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.AuditLogEntity;
import java.util.List;

public interface AuditLogRepositoryCustom {

    List<AuditLogEntity> findQuotationHistory(String quotationId);
}
