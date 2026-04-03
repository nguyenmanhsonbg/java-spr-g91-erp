ALTER TABLE payments
    ADD COLUMN note VARCHAR(500) NULL,
    ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE payments
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

CREATE TABLE debt_reminders (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    invoice_id CHAR(36) NOT NULL,
    reminder_type VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    note VARCHAR(500) NULL,
    sent_by CHAR(36) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_debt_reminder_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_debt_reminder_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id)
) ENGINE=InnoDB;

CREATE INDEX idx_debt_reminders_customer_sent_at
    ON debt_reminders(customer_id, sent_at);

CREATE INDEX idx_debt_reminders_invoice_sent_at
    ON debt_reminders(invoice_id, sent_at);

CREATE TABLE debt_settlements (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    settlement_date DATE NOT NULL,
    confirmed_by CHAR(36) NOT NULL,
    note VARCHAR(500) NULL,
    certificate_url VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_debt_settlement_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE INDEX idx_debt_settlements_customer_date
    ON debt_settlements(customer_id, settlement_date);
