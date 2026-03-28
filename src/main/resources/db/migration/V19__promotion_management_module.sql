ALTER TABLE promotions
    ADD COLUMN priority INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN description VARCHAR(1000) NULL AFTER priority,
    ADD COLUMN updated_by CHAR(36) NULL AFTER created_by,
    ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP AFTER updated_by,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER updated_at;

ALTER TABLE promotion_products
    ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP AFTER product_id,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER created_at;

CREATE TABLE promotion_customer_groups (
    id CHAR(36) PRIMARY KEY,
    promotion_id CHAR(36) NOT NULL,
    customer_group VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_promotion_customer_group_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id)
) ENGINE=InnoDB;

CREATE INDEX idx_promotions_status_dates
    ON promotions(status, deleted_at, start_date, end_date, priority);

CREATE INDEX idx_promotion_products_lookup
    ON promotion_products(promotion_id, product_id, deleted_at);

CREATE INDEX idx_promotion_customer_groups_lookup
    ON promotion_customer_groups(promotion_id, customer_group, deleted_at);
