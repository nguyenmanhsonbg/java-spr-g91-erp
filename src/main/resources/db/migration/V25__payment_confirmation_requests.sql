ALTER TABLE payments
    MODIFY COLUMN note VARCHAR(1000) NULL,
    ADD COLUMN status VARCHAR(20) NULL AFTER note,
    ADD COLUMN proof_document_url VARCHAR(1000) NULL AFTER status,
    ADD COLUMN updated_by CHAR(36) NULL AFTER created_by,
    ADD COLUMN updated_at TIMESTAMP NULL AFTER created_at;

UPDATE payments
SET status = 'CONFIRMED',
    updated_at = COALESCE(updated_at, created_at)
WHERE status IS NULL;

CREATE TABLE payment_confirmation_requests (
    id CHAR(36) PRIMARY KEY,
    invoice_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    requested_amount DECIMAL(18,2) NOT NULL,
    confirmed_amount DECIMAL(18,2) NULL,
    payment_id CHAR(36) NULL,
    transfer_time TIMESTAMP NOT NULL,
    sender_bank_name VARCHAR(255) NOT NULL,
    sender_account_name VARCHAR(255) NOT NULL,
    sender_account_no VARCHAR(100) NOT NULL,
    reference_code VARCHAR(100) NOT NULL,
    proof_document_url VARCHAR(1000) NULL,
    note VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL,
    review_note VARCHAR(1000) NULL,
    reviewed_by CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pending_invoice_id CHAR(36)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'PENDING_REVIEW' THEN invoice_id
                ELSE NULL
            END
        ) STORED,
    CONSTRAINT fk_payment_confirmation_request_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_payment_confirmation_request_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_payment_confirmation_request_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB;

CREATE UNIQUE INDEX uk_payment_confirmation_request_pending_invoice
    ON payment_confirmation_requests(pending_invoice_id);

CREATE INDEX idx_payment_confirmation_request_customer_created_at
    ON payment_confirmation_requests(customer_id, created_at);

CREATE INDEX idx_payment_confirmation_request_invoice_created_at
    ON payment_confirmation_requests(invoice_id, created_at);

CREATE INDEX idx_payment_confirmation_request_status_created_at
    ON payment_confirmation_requests(status, created_at);
