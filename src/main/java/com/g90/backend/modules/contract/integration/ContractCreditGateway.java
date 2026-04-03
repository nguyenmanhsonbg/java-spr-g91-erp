package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

public interface ContractCreditGateway {

    CreditSnapshot getCreditSnapshot(CustomerProfileEntity customer);

    record CreditSnapshot(
            BigDecimal creditLimit,
            BigDecimal currentDebt,
            BigDecimal availableCredit,
            boolean exceeded
    ) {
    }
}

@Component
class JpaContractCreditGateway implements ContractCreditGateway {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CreditSnapshot getCreditSnapshot(CustomerProfileEntity customer) {
        BigDecimal creditLimit = customer == null || customer.getCreditLimit() == null
                ? BigDecimal.ZERO.setScale(2)
                : customer.getCreditLimit().setScale(2);
        BigDecimal currentDebt = BigDecimal.ZERO.setScale(2);
        if (customer != null) {
            Object result = entityManager.createNativeQuery("""
                    SELECT COALESCE(SUM((i.total_amount + COALESCE(i.vat_amount, 0)) - COALESCE(pa.allocated_amount, 0)), 0)
                    FROM invoices i
                    LEFT JOIN (
                        SELECT p.invoice_id, SUM(p.amount) AS allocated_amount
                        FROM payment_allocations p
                        GROUP BY p.invoice_id
                    ) pa ON pa.invoice_id = i.id
                    WHERE i.customer_id = :customerId
                      AND UPPER(COALESCE(i.status, 'OPEN')) NOT IN ('PAID', 'CANCELLED', 'VOID', 'DRAFT')
                    """)
                    .setParameter("customerId", customer.getId())
                    .getSingleResult();
            currentDebt = toDecimal(result);
        }
        BigDecimal availableCredit = creditLimit.subtract(currentDebt).setScale(2);
        boolean exceeded = creditLimit.compareTo(BigDecimal.ZERO) > 0 && availableCredit.compareTo(BigDecimal.ZERO) < 0;
        return new CreditSnapshot(creditLimit, currentDebt, availableCredit, exceeded);
    }

    private BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2);
        }
        return BigDecimal.ZERO.setScale(2);
    }
}
