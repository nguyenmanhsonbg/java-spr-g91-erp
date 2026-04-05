ALTER TABLE contracts
    ADD COLUMN sale_order_number VARCHAR(50) NULL AFTER contract_number,
    ADD COLUMN actual_delivery_date DATE NULL AFTER expected_delivery_date;

ALTER TABLE contract_items
    ADD COLUMN reserved_quantity DECIMAL(18,2) NULL AFTER quantity,
    ADD COLUMN issued_quantity DECIMAL(18,2) NULL AFTER reserved_quantity,
    ADD COLUMN delivered_quantity DECIMAL(18,2) NULL AFTER issued_quantity,
    ADD COLUMN fulfillment_note VARCHAR(500) NULL AFTER delivered_quantity;

UPDATE contracts
SET sale_order_number = CONCAT('SO-', SUBSTRING(contract_number, 4))
WHERE submitted_at IS NOT NULL
  AND sale_order_number IS NULL
  AND contract_number LIKE 'CT-%';

UPDATE contract_items ci
JOIN contracts c ON c.id = ci.contract_id
SET ci.reserved_quantity = CASE
        WHEN c.status IN ('SUBMITTED', 'PROCESSING', 'RESERVED', 'PICKED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED') THEN ci.quantity
        ELSE 0
    END,
    ci.issued_quantity = CASE
        WHEN c.status IN ('PICKED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED') THEN ci.quantity
        ELSE 0
    END,
    ci.delivered_quantity = CASE
        WHEN c.status IN ('DELIVERED', 'COMPLETED') THEN ci.quantity
        ELSE 0
    END
WHERE ci.reserved_quantity IS NULL
   OR ci.issued_quantity IS NULL
   OR ci.delivered_quantity IS NULL;

UPDATE contract_items
SET reserved_quantity = COALESCE(reserved_quantity, 0),
    issued_quantity = COALESCE(issued_quantity, 0),
    delivered_quantity = COALESCE(delivered_quantity, 0);

CREATE UNIQUE INDEX uk_contracts_sale_order_number
    ON contracts(sale_order_number);

CREATE INDEX idx_contracts_sale_order_status
    ON contracts(sale_order_number, status, submitted_at);

CREATE INDEX idx_inventory_transactions_related_order
    ON inventory_transactions(related_order_id, transaction_date);
