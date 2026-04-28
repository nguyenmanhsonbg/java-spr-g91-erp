ALTER TABLE invoices
    ADD COLUMN billing_phase VARCHAR(30) NULL AFTER source_type;

CREATE INDEX idx_invoices_contract_billing_phase
    ON invoices(contract_id, billing_phase, status);

CREATE TABLE contract_cancellation_settlements (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NULL,
    settlement_type VARCHAR(40) NOT NULL,
    deposit_refund_amount DECIMAL(18,2) NULL,
    compensation_amount DECIMAL(18,2) NULL,
    forfeited_deposit_amount DECIMAL(18,2) NULL,
    total_payable_amount DECIMAL(18,2) NULL,
    status VARCHAR(20) NULL,
    note VARCHAR(1000) NULL,
    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_contract_cancellation_settlement_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_contract_cancellation_settlement_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE INDEX idx_contract_cancellation_settlements_contract
    ON contract_cancellation_settlements(contract_id, created_at);
