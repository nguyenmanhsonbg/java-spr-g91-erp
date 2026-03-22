-- Seed a default customer price group so newly created customers can immediately resolve pricing.

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
    'f1500000-0000-0000-0000-000000000001',
    'Bang Gia Mac Dinh',
    'DEFAULT',
    NULL,
    NULL,
    'ACTIVE',
    NULL,
    CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM price_lists
    WHERE id = 'f1500000-0000-0000-0000-000000000001'
       OR UPPER(customer_group) = 'DEFAULT'
);

INSERT INTO price_list_items (
    id,
    price_list_id,
    product_id,
    unit_price
)
SELECT
    UUID(),
    target_list.id,
    source_items.product_id,
    source_items.unit_price
FROM price_list_items source_items
JOIN price_lists source_list
    ON source_list.id = source_items.price_list_id
JOIN (
    SELECT pl.id
    FROM price_lists pl
    WHERE UPPER(pl.customer_group) = 'DEFAULT'
    ORDER BY pl.created_at DESC, pl.id DESC
    LIMIT 1
) target_list
WHERE UPPER(source_list.customer_group) = 'CONTRACTOR'
  AND UPPER(source_list.status) = 'ACTIVE'
  AND source_list.id = (
      SELECT selected.id
      FROM (
          SELECT pl.id
          FROM price_lists pl
          WHERE UPPER(pl.customer_group) = 'CONTRACTOR'
            AND UPPER(pl.status) = 'ACTIVE'
          ORDER BY pl.start_date DESC, pl.created_at DESC, pl.id DESC
          LIMIT 1
      ) selected
  )
  AND NOT EXISTS (
      SELECT 1
      FROM price_list_items target_items
      WHERE target_items.price_list_id = target_list.id
        AND target_items.product_id = source_items.product_id
  );
