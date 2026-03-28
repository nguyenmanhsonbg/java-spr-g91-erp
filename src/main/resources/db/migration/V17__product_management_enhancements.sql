ALTER TABLE products
    ADD COLUMN description VARCHAR(1000) NULL AFTER reference_weight,
    ADD COLUMN created_by CHAR(36) NULL AFTER status,
    ADD COLUMN updated_by CHAR(36) NULL AFTER created_by,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER updated_at;

UPDATE products
SET status = COALESCE(status, 'ACTIVE'),
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE status IS NULL
   OR updated_at IS NULL;

CREATE TABLE product_images (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE INDEX idx_products_catalog_lookup
ON products(status, deleted_at, type, size, thickness);

CREATE INDEX idx_product_images_product_order
ON product_images(product_id, display_order);
