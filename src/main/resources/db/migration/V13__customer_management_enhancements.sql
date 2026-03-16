ALTER TABLE customers
    ADD COLUMN customer_code VARCHAR(50) NULL AFTER id,
    ADD COLUMN price_group VARCHAR(50) NULL AFTER customer_type,
    ADD COLUMN payment_terms VARCHAR(255) NULL AFTER credit_limit,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

SET @customer_seq := 0;

UPDATE customers c
JOIN (
    SELECT id
    FROM customers
    ORDER BY created_at, id
) ordered ON ordered.id = c.id
SET c.customer_code = CONCAT('CUST-', LPAD((@customer_seq := @customer_seq + 1), 3, '0'))
WHERE c.customer_code IS NULL;

UPDATE customers
SET price_group = customer_type
WHERE price_group IS NULL
  AND customer_type IS NOT NULL;

UPDATE customers
SET payment_terms = '70% on delivery, 30% within 30 days'
WHERE payment_terms IS NULL;

UPDATE customers
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE customers
    ADD CONSTRAINT uk_customers_customer_code UNIQUE (customer_code);
