ALTER TABLE invoices
    ADD COLUMN source_type VARCHAR(30) NULL AFTER customer_id,
    ADD COLUMN customer_name VARCHAR(255) NULL AFTER source_type,
    ADD COLUMN customer_tax_code VARCHAR(20) NULL AFTER customer_name,
    ADD COLUMN billing_address VARCHAR(500) NULL AFTER customer_tax_code,
    ADD COLUMN issue_date DATE NULL AFTER billing_address,
    ADD COLUMN payment_terms VARCHAR(255) NULL AFTER issue_date,
    ADD COLUMN note VARCHAR(1000) NULL AFTER payment_terms,
    ADD COLUMN adjustment_amount DECIMAL(18,2) NULL AFTER note,
    ADD COLUMN vat_rate DECIMAL(5,2) NULL AFTER adjustment_amount,
    ADD COLUMN document_url VARCHAR(1000) NULL AFTER vat_rate,
    ADD COLUMN created_by CHAR(36) NULL AFTER document_url,
    ADD COLUMN updated_by CHAR(36) NULL AFTER created_by,
    ADD COLUMN issued_by CHAR(36) NULL AFTER updated_by,
    ADD COLUMN cancelled_by CHAR(36) NULL AFTER issued_by,
    ADD COLUMN cancellation_reason VARCHAR(1000) NULL AFTER cancelled_by,
    ADD COLUMN updated_at TIMESTAMP NULL AFTER created_at,
    ADD COLUMN issued_at TIMESTAMP NULL AFTER updated_at,
    ADD COLUMN cancelled_at TIMESTAMP NULL AFTER issued_at,
    ADD COLUMN notification_sent_at TIMESTAMP NULL AFTER cancelled_at;

ALTER TABLE invoice_items
    ADD COLUMN description VARCHAR(255) NULL AFTER product_id,
    ADD COLUMN unit VARCHAR(20) NULL AFTER description;
