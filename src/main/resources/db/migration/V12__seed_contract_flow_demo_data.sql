-- Seed prerequisite data for the UI flow:
-- Customer creates quotation -> Accountant creates contract -> Owner approves contract.
--
-- This migration does not pre-create quotations or contracts.
-- It only seeds the minimum demo data needed so users can reproduce the flow manually on UI.

-- =========================================================
-- ACTIVE PRICE LIST FOR CONTRACTOR CUSTOMER GROUP
-- =========================================================

INSERT INTO price_lists (
    id,
    name,
    customer_group,
    start_date,
    end_date,
    status,
    created_by,
    created_at
)
SELECT
    'f1200000-0000-0000-0000-000000000001',
    'Bang Gia Demo Flow Contract Approval',
    'CONTRACTOR',
    '2026-01-01',
    '2027-12-31',
    'ACTIVE',
    '55555555-5555-5555-5555-555555555555',
    CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM price_lists
    WHERE id = 'f1200000-0000-0000-0000-000000000001'
       OR name = 'Bang Gia Demo Flow Contract Approval'
);

INSERT INTO price_list_items (
    id,
    price_list_id,
    product_id,
    unit_price
)
SELECT
    'f1200000-0000-0000-0000-000000000101',
    'f1200000-0000-0000-0000-000000000001',
    '3c08d8f0-6a1d-4c54-9b40-000000000001',
    9500000.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM price_lists
    WHERE id = 'f1200000-0000-0000-0000-000000000001'
)
AND EXISTS (
    SELECT 1
    FROM products
    WHERE id = '3c08d8f0-6a1d-4c54-9b40-000000000001'
      AND UPPER(status) = 'ACTIVE'
)
AND NOT EXISTS (
    SELECT 1
    FROM price_list_items
    WHERE price_list_id = 'f1200000-0000-0000-0000-000000000001'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000001'
);

INSERT INTO price_list_items (
    id,
    price_list_id,
    product_id,
    unit_price
)
SELECT
    'f1200000-0000-0000-0000-000000000102',
    'f1200000-0000-0000-0000-000000000001',
    '3c08d8f0-6a1d-4c54-9b40-000000000002',
    10200000.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM price_lists
    WHERE id = 'f1200000-0000-0000-0000-000000000001'
)
AND EXISTS (
    SELECT 1
    FROM products
    WHERE id = '3c08d8f0-6a1d-4c54-9b40-000000000002'
      AND UPPER(status) = 'ACTIVE'
)
AND NOT EXISTS (
    SELECT 1
    FROM price_list_items
    WHERE price_list_id = 'f1200000-0000-0000-0000-000000000001'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000002'
);

INSERT INTO price_list_items (
    id,
    price_list_id,
    product_id,
    unit_price
)
SELECT
    'f1200000-0000-0000-0000-000000000103',
    'f1200000-0000-0000-0000-000000000001',
    '3c08d8f0-6a1d-4c54-9b40-000000000003',
    20000000.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM price_lists
    WHERE id = 'f1200000-0000-0000-0000-000000000001'
)
AND EXISTS (
    SELECT 1
    FROM products
    WHERE id = '3c08d8f0-6a1d-4c54-9b40-000000000003'
      AND UPPER(status) = 'ACTIVE'
)
AND NOT EXISTS (
    SELECT 1
    FROM price_list_items
    WHERE price_list_id = 'f1200000-0000-0000-0000-000000000001'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000003'
);

INSERT INTO price_list_items (
    id,
    price_list_id,
    product_id,
    unit_price
)
SELECT
    'f1200000-0000-0000-0000-000000000104',
    'f1200000-0000-0000-0000-000000000001',
    '3c08d8f0-6a1d-4c54-9b40-000000000004',
    21500000.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM price_lists
    WHERE id = 'f1200000-0000-0000-0000-000000000001'
)
AND EXISTS (
    SELECT 1
    FROM products
    WHERE id = '3c08d8f0-6a1d-4c54-9b40-000000000004'
      AND UPPER(status) = 'ACTIVE'
)
AND NOT EXISTS (
    SELECT 1
    FROM price_list_items
    WHERE price_list_id = 'f1200000-0000-0000-0000-000000000001'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000004'
);

-- =========================================================
-- INVENTORY FOR CONTRACT SUBMISSION AND STOCK CHECK
-- =========================================================

INSERT INTO inventory (
    id,
    warehouse_id,
    product_id,
    quantity
)
SELECT
    'f1200000-0000-0000-0000-000000000201',
    'a1111111-1111-1111-1111-111111111111',
    '3c08d8f0-6a1d-4c54-9b40-000000000001',
    500.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM warehouses
    WHERE id = 'a1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1
    FROM inventory
    WHERE warehouse_id = 'a1111111-1111-1111-1111-111111111111'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000001'
);

INSERT INTO inventory (
    id,
    warehouse_id,
    product_id,
    quantity
)
SELECT
    'f1200000-0000-0000-0000-000000000202',
    'a1111111-1111-1111-1111-111111111111',
    '3c08d8f0-6a1d-4c54-9b40-000000000002',
    500.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM warehouses
    WHERE id = 'a1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1
    FROM inventory
    WHERE warehouse_id = 'a1111111-1111-1111-1111-111111111111'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000002'
);

INSERT INTO inventory (
    id,
    warehouse_id,
    product_id,
    quantity
)
SELECT
    'f1200000-0000-0000-0000-000000000203',
    'a1111111-1111-1111-1111-111111111111',
    '3c08d8f0-6a1d-4c54-9b40-000000000003',
    500.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM warehouses
    WHERE id = 'a1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1
    FROM inventory
    WHERE warehouse_id = 'a1111111-1111-1111-1111-111111111111'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000003'
);

INSERT INTO inventory (
    id,
    warehouse_id,
    product_id,
    quantity
)
SELECT
    'f1200000-0000-0000-0000-000000000204',
    'a1111111-1111-1111-1111-111111111111',
    '3c08d8f0-6a1d-4c54-9b40-000000000004',
    500.00
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM warehouses
    WHERE id = 'a1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1
    FROM inventory
    WHERE warehouse_id = 'a1111111-1111-1111-1111-111111111111'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000004'
);

-- =========================================================
-- OPTIONAL PROMOTION FOR UI DEMO
-- =========================================================

INSERT INTO promotions (
    id,
    code,
    name,
    promotion_type,
    discount_value,
    start_date,
    end_date,
    status,
    created_by
)
SELECT
    'f1200000-0000-0000-0000-000000000301',
    'FLOWPROMO5',
    'Khuyen Mai Demo Flow 5 Phan Tram',
    'PERCENT',
    5.00,
    '2026-01-01',
    '2027-12-31',
    'ACTIVE',
    '55555555-5555-5555-5555-555555555555'
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM promotions
    WHERE id = 'f1200000-0000-0000-0000-000000000301'
       OR UPPER(code) = 'FLOWPROMO5'
);

INSERT INTO promotion_products (
    id,
    promotion_id,
    product_id
)
SELECT
    'f1200000-0000-0000-0000-000000000401',
    'f1200000-0000-0000-0000-000000000301',
    '3c08d8f0-6a1d-4c54-9b40-000000000003'
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM promotions
    WHERE id = 'f1200000-0000-0000-0000-000000000301'
)
AND NOT EXISTS (
    SELECT 1
    FROM promotion_products
    WHERE promotion_id = 'f1200000-0000-0000-0000-000000000301'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000003'
);

INSERT INTO promotion_products (
    id,
    promotion_id,
    product_id
)
SELECT
    'f1200000-0000-0000-0000-000000000402',
    'f1200000-0000-0000-0000-000000000301',
    '3c08d8f0-6a1d-4c54-9b40-000000000004'
FROM dual
WHERE EXISTS (
    SELECT 1
    FROM promotions
    WHERE id = 'f1200000-0000-0000-0000-000000000301'
)
AND NOT EXISTS (
    SELECT 1
    FROM promotion_products
    WHERE promotion_id = 'f1200000-0000-0000-0000-000000000301'
      AND product_id = '3c08d8f0-6a1d-4c54-9b40-000000000004'
);
