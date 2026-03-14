package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String>, AuditLogRepositoryCustom {
}
