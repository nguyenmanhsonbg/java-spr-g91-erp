package com.g90.backend.modules.customer.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerAnalyticsRepositoryImpl implements CustomerAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CustomerAggregateSnapshot getAggregateSnapshot(String customerId) {
        return new CustomerAggregateSnapshot(
                queryLong("select count(*) from quotations where customer_id = :customerId", customerId),
                queryLong("select count(*) from contracts where customer_id = :customerId", customerId),
                queryLong("select count(*) from invoices where customer_id = :customerId", customerId),
                queryLong("select count(*) from projects where customer_id = :customerId", customerId),
                queryLong(
                        """
                        select count(*)
                        from projects
                        where customer_id = :customerId
                          and upper(coalesce(status, '')) not in ('CLOSED', 'ARCHIVED')
                        """,
                        customerId
                ),
                queryLong(
                        """
                        select count(*)
                        from contracts
                        where customer_id = :customerId
                          and upper(coalesce(status, '')) not in ('COMPLETED', 'CANCELLED', 'REJECTED')
                        """,
                        customerId
                ),
                queryMoney(
                        """
                        select coalesce(sum(coalesce(total_amount, 0) + coalesce(vat_amount, 0)), 0)
                        from invoices
                        where customer_id = :customerId
                          and upper(coalesce(status, '')) not in ('DRAFT', 'CANCELLED', 'VOID')
                        """,
                        customerId
                ),
                queryMoney("select coalesce(sum(coalesce(amount, 0)), 0) from payments where customer_id = :customerId", customerId),
                queryMoney(
                        """
                        select coalesce(sum(coalesce(pa.amount, 0)), 0)
                        from payment_allocations pa
                        join invoices i on i.id = pa.invoice_id
                        where i.customer_id = :customerId
                        """,
                        customerId
                ),
                queryDateTime(
                        """
                        select max(tx.event_at)
                        from (
                            select q.created_at as event_at from quotations q where q.customer_id = :customerId
                            union all
                            select c.created_at as event_at from contracts c where c.customer_id = :customerId
                            union all
                            select p.created_at as event_at from projects p where p.customer_id = :customerId
                            union all
                            select i.created_at as event_at from invoices i where i.customer_id = :customerId
                            union all
                            select cast(pm.payment_date as datetime) as event_at from payments pm where pm.customer_id = :customerId
                        ) tx
                        """,
                        customerId
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CustomerRecentTransactionSnapshot> getRecentTransactions(String customerId, int limit) {
        List<Object[]> rows = entityManager.createNativeQuery(
                        """
                        select tx.tx_type, tx.entity_id, tx.reference_no, tx.status, tx.amount, tx.event_at
                        from (
                            select 'QUOTATION' as tx_type,
                                   q.id as entity_id,
                                   q.quotation_number as reference_no,
                                   q.status as status,
                                   coalesce(q.total_amount, 0) as amount,
                                   q.created_at as event_at
                            from quotations q
                            where q.customer_id = :customerId

                            union all

                            select 'CONTRACT' as tx_type,
                                   c.id as entity_id,
                                   c.contract_number as reference_no,
                                   c.status as status,
                                   coalesce(c.total_amount, 0) as amount,
                                   c.created_at as event_at
                            from contracts c
                            where c.customer_id = :customerId

                            union all

                            select 'PROJECT' as tx_type,
                                   p.id as entity_id,
                                   p.project_code as reference_no,
                                   p.status as status,
                                   coalesce(p.budget, 0) as amount,
                                   p.created_at as event_at
                            from projects p
                            where p.customer_id = :customerId

                            union all

                            select 'INVOICE' as tx_type,
                                   i.id as entity_id,
                                   i.invoice_number as reference_no,
                                   i.status as status,
                                   coalesce(i.total_amount, 0) + coalesce(i.vat_amount, 0) as amount,
                                   i.created_at as event_at
                            from invoices i
                            where i.customer_id = :customerId

                            union all

                            select 'PAYMENT' as tx_type,
                                   pm.id as entity_id,
                                   coalesce(pm.reference_no, pm.id) as reference_no,
                                   coalesce(pm.payment_method, 'RECORDED') as status,
                                   coalesce(pm.amount, 0) as amount,
                                   cast(pm.payment_date as datetime) as event_at
                            from payments pm
                            where pm.customer_id = :customerId
                        ) tx
                        order by tx.event_at desc
                        """)
                .setParameter("customerId", customerId)
                .setMaxResults(Math.max(1, limit))
                .getResultList();

        return rows.stream()
                .map(this::toRecentTransactionSnapshot)
                .toList();
    }

    private CustomerRecentTransactionSnapshot toRecentTransactionSnapshot(Object[] row) {
        return new CustomerRecentTransactionSnapshot(
                asString(row[0]),
                asString(row[1]),
                asString(row[2]),
                asString(row[3]),
                asBigDecimal(row[4]),
                asLocalDateTime(row[5])
        );
    }

    private long queryLong(String sql, String customerId) {
        Object value = entityManager.createNativeQuery(sql)
                .setParameter("customerId", customerId)
                .getSingleResult();
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private BigDecimal queryMoney(String sql, String customerId) {
        return asBigDecimal(entityManager.createNativeQuery(sql)
                .setParameter("customerId", customerId)
                .getSingleResult());
    }

    private LocalDateTime queryDateTime(String sql, String customerId) {
        return asLocalDateTime(entityManager.createNativeQuery(sql)
                .setParameter("customerId", customerId)
                .getSingleResult());
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        return Timestamp.valueOf(value.toString()).toLocalDateTime();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
