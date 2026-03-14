package com.g90.backend.modules.account.repository;

import com.g90.backend.modules.account.entity.AuditLogEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<AuditLogEntity> findQuotationHistory(String quotationId) {
        return entityManager.createQuery("""
                select a
                from AuditLogEntity a
                where a.entityType = 'QUOTATION'
                  and a.entityId = :quotationId
                order by a.createdAt asc
                """, AuditLogEntity.class)
                .setParameter("quotationId", quotationId)
                .getResultList();
    }
}
