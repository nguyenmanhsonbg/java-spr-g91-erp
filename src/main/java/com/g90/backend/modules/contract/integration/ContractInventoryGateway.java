package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.contract.entity.ContractEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

public interface ContractInventoryGateway {

    Map<String, InventoryAvailability> checkAvailability(Collection<RequestedInventory> requests);

    ReservationOutcome reserveInventory(ContractEntity contract);

    ReservationOutcome releaseReservation(ContractEntity contract, String reason);

    record RequestedInventory(String productId, BigDecimal requiredQuantity) {
    }

    record InventoryAvailability(String productId, BigDecimal availableQuantity, boolean sufficient) {
    }

    record ReservationOutcome(boolean reserved, String reservationNote) {
    }
}

@Component
class JpaContractInventoryGateway implements ContractInventoryGateway {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, InventoryAvailability> checkAvailability(Collection<RequestedInventory> requests) {
        Map<String, InventoryAvailability> results = new LinkedHashMap<>();
        if (requests == null || requests.isEmpty()) {
            return results;
        }

        for (RequestedInventory request : requests) {
            Object rawQuantity = entityManager.createNativeQuery("""
                    SELECT COALESCE(SUM(i.quantity), 0)
                    FROM inventory i
                    WHERE i.product_id = :productId
                    """)
                    .setParameter("productId", request.productId())
                    .getSingleResult();
            BigDecimal available = toDecimal(rawQuantity);
            boolean sufficient = available.compareTo(normalize(request.requiredQuantity())) >= 0;
            results.put(request.productId(), new InventoryAvailability(request.productId(), available, sufficient));
        }
        return results;
    }

    @Override
    public ReservationOutcome reserveInventory(ContractEntity contract) {
        // Reservation is scheduler/inventory-module ready: current implementation only acknowledges the request.
        return new ReservationOutcome(true, "Inventory reservation requested for downstream fulfillment");
    }

    @Override
    public ReservationOutcome releaseReservation(ContractEntity contract, String reason) {
        return new ReservationOutcome(true, "Inventory release requested: " + reason);
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

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }
}
