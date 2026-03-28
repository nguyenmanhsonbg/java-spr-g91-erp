ALTER TABLE price_lists
    ADD COLUMN updated_by CHAR(36) NULL AFTER created_by,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER updated_at;

UPDATE price_lists
SET status = COALESCE(status, 'ACTIVE'),
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE status IS NULL
   OR updated_at IS NULL;

ALTER TABLE price_list_items
    ADD COLUMN pricing_rule_type VARCHAR(30) NULL AFTER unit_price,
    ADD COLUMN note VARCHAR(500) NULL AFTER pricing_rule_type,
    ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP AFTER note,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER updated_at;

UPDATE price_list_items
SET pricing_rule_type = COALESCE(pricing_rule_type, 'FIXED_PRICE'),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE pricing_rule_type IS NULL
   OR created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE quotations
    ADD COLUMN price_list_id CHAR(36) NULL AFTER project_id,
    ADD CONSTRAINT fk_quotations_price_list
        FOREIGN KEY (price_list_id) REFERENCES price_lists(id);

UPDATE quotations q
JOIN customers c ON c.id = q.customer_id
SET q.price_list_id = (
    SELECT selected.id
    FROM (
        SELECT pl.id
        FROM price_lists pl
        WHERE UPPER(pl.customer_group) = UPPER(COALESCE(NULLIF(c.price_group, ''), c.customer_type))
          AND UPPER(COALESCE(pl.status, 'ACTIVE')) = 'ACTIVE'
          AND (pl.deleted_at IS NULL)
          AND (pl.start_date IS NULL OR pl.start_date <= DATE(q.created_at))
          AND (pl.end_date IS NULL OR pl.end_date >= DATE(q.created_at))
        ORDER BY pl.start_date DESC, pl.created_at DESC, pl.id DESC
        LIMIT 1
    ) selected
)
WHERE q.price_list_id IS NULL;

ALTER TABLE contracts
    ADD COLUMN price_list_id CHAR(36) NULL AFTER quotation_id,
    ADD CONSTRAINT fk_contracts_price_list
        FOREIGN KEY (price_list_id) REFERENCES price_lists(id);

UPDATE contracts c
LEFT JOIN quotations q ON q.id = c.quotation_id
LEFT JOIN customers customer_profile ON customer_profile.id = c.customer_id
SET c.price_list_id = COALESCE(
    q.price_list_id,
    (
        SELECT selected.id
        FROM (
            SELECT pl.id
            FROM price_lists pl
            WHERE UPPER(pl.customer_group) = UPPER(COALESCE(NULLIF(customer_profile.price_group, ''), customer_profile.customer_type))
              AND UPPER(COALESCE(pl.status, 'ACTIVE')) = 'ACTIVE'
              AND (pl.deleted_at IS NULL)
              AND (pl.start_date IS NULL OR pl.start_date <= DATE(c.created_at))
              AND (pl.end_date IS NULL OR pl.end_date >= DATE(c.created_at))
            ORDER BY pl.start_date DESC, pl.created_at DESC, pl.id DESC
            LIMIT 1
        ) selected
    )
)
WHERE c.price_list_id IS NULL;

CREATE INDEX idx_price_lists_lookup
ON price_lists(customer_group, status, deleted_at, start_date, end_date);

CREATE INDEX idx_price_list_items_lookup
ON price_list_items(price_list_id, deleted_at, product_id);

CREATE INDEX idx_quotations_price_list_id
ON quotations(price_list_id);

CREATE INDEX idx_contracts_price_list_status
ON contracts(price_list_id, status);
