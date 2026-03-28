ALTER TABLE inventory
    ADD COLUMN updated_by CHAR(36) NULL AFTER updated_at;

ALTER TABLE inventory_transactions
    ADD COLUMN transaction_code VARCHAR(50) NULL AFTER id,
    ADD COLUMN quantity_before DECIMAL(18,2) NULL AFTER quantity,
    ADD COLUMN quantity_after DECIMAL(18,2) NULL AFTER quantity_before,
    ADD COLUMN transaction_date DATETIME NULL AFTER quantity_after,
    ADD COLUMN supplier_name VARCHAR(255) NULL AFTER transaction_date,
    ADD COLUMN related_order_id CHAR(36) NULL AFTER supplier_name,
    ADD COLUMN related_project_id CHAR(36) NULL AFTER related_order_id,
    ADD COLUMN note VARCHAR(500) NULL AFTER reason;

UPDATE inventory_transactions
SET transaction_date = COALESCE(transaction_date, created_at)
WHERE transaction_date IS NULL;

CREATE INDEX idx_inventory_product_updated
ON inventory(product_id, updated_at);

CREATE INDEX idx_inventory_transactions_lookup
ON inventory_transactions(product_id, transaction_type, transaction_date);

CREATE UNIQUE INDEX idx_inventory_transactions_code
ON inventory_transactions(transaction_code);
