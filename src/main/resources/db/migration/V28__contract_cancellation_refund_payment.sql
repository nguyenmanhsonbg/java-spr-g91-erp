ALTER TABLE contract_cancellation_settlements
    ADD COLUMN paid_amount DECIMAL(18,2) NULL AFTER total_payable_amount,
    ADD COLUMN payment_method VARCHAR(50) NULL AFTER status,
    ADD COLUMN reference_no VARCHAR(100) NULL AFTER payment_method,
    ADD COLUMN proof_document_url VARCHAR(1000) NULL AFTER reference_no,
    ADD COLUMN payment_note VARCHAR(1000) NULL AFTER note,
    ADD COLUMN paid_by CHAR(36) NULL AFTER updated_by,
    ADD COLUMN paid_at TIMESTAMP NULL AFTER paid_by;

CREATE INDEX idx_contract_cancellation_settlements_status
    ON contract_cancellation_settlements(status, settlement_type, created_at);
