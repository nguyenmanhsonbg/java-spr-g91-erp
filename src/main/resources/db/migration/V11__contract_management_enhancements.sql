ALTER TABLE audit_logs
ADD INDEX idx_audit_logs_entity_lookup (entity_type, entity_id, created_at);

ALTER TABLE contracts
ADD COLUMN note VARCHAR(1000) NULL,
ADD COLUMN delivery_terms VARCHAR(1000) NULL,
ADD COLUMN expected_delivery_date DATE NULL,
ADD COLUMN confidential BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN approval_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
ADD COLUMN approval_tier VARCHAR(30) NULL,
ADD COLUMN pending_action VARCHAR(30) NULL,
ADD COLUMN approval_requested_at TIMESTAMP NULL,
ADD COLUMN approval_due_at TIMESTAMP NULL,
ADD COLUMN approved_by CHAR(36) NULL,
ADD COLUMN approved_at TIMESTAMP NULL,
ADD COLUMN credit_limit_snapshot DECIMAL(18,2) NULL,
ADD COLUMN current_debt_snapshot DECIMAL(18,2) NULL,
ADD COLUMN deposit_percentage DECIMAL(5,2) NULL,
ADD COLUMN deposit_amount DECIMAL(18,2) NULL,
ADD COLUMN submitted_by CHAR(36) NULL,
ADD COLUMN submitted_at TIMESTAMP NULL,
ADD COLUMN cancelled_by CHAR(36) NULL,
ADD COLUMN cancelled_at TIMESTAMP NULL,
ADD COLUMN cancellation_reason_code VARCHAR(50) NULL,
ADD COLUMN cancellation_note VARCHAR(1000) NULL,
ADD COLUMN updated_by CHAR(36) NULL,
ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
ADD COLUMN auto_submit_due_at TIMESTAMP NULL,
ADD COLUMN price_change_percent DECIMAL(10,2) NULL DEFAULT 0,
ADD COLUMN last_status_change_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN last_tracking_refresh_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE contracts
ADD CONSTRAINT uk_contract_number UNIQUE (contract_number);

CREATE INDEX idx_contracts_customer_status ON contracts(customer_id, status);
CREATE INDEX idx_contracts_approval_status ON contracts(approval_status, approval_requested_at);
CREATE INDEX idx_contracts_confidential_created_at ON contracts(confidential, created_at);
CREATE INDEX idx_contracts_expected_delivery_date ON contracts(expected_delivery_date);

ALTER TABLE contract_items
ADD COLUMN base_unit_price DECIMAL(18,2) NULL,
ADD COLUMN discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
ADD COLUMN price_override_reason VARCHAR(500) NULL;

CREATE TABLE contract_versions (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    version_no INT NOT NULL,
    change_reason VARCHAR(500) NOT NULL,
    snapshot JSON NOT NULL,
    changed_by CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_versions_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT uk_contract_versions_contract_version
        UNIQUE (contract_id, version_no)
) ENGINE=InnoDB;

CREATE TABLE contract_approvals (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    approval_type VARCHAR(30) NOT NULL,
    approval_tier VARCHAR(30) NULL,
    status VARCHAR(30) NOT NULL,
    requested_by CHAR(36) NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_at TIMESTAMP NULL,
    decided_by CHAR(36) NULL,
    decided_at TIMESTAMP NULL,
    comment VARCHAR(1000) NULL,
    CONSTRAINT fk_contract_approvals_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id)
) ENGINE=InnoDB;

CREATE INDEX idx_contract_approvals_status_due_at
ON contract_approvals(status, due_at, requested_at);

CREATE TABLE contract_status_history (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    change_reason VARCHAR(500) NULL,
    changed_by CHAR(36) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_status_history_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id)
) ENGINE=InnoDB;

CREATE INDEX idx_contract_status_history_contract_changed_at
ON contract_status_history(contract_id, changed_at);

CREATE TABLE contract_documents (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    document_number VARCHAR(50) NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NULL,
    preview_only BOOLEAN NOT NULL DEFAULT TRUE,
    official_document BOOLEAN NOT NULL DEFAULT FALSE,
    watermark_text VARCHAR(50) NULL,
    generation_status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    export_count INT NOT NULL DEFAULT 0,
    generated_by CHAR(36) NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_exported_by CHAR(36) NULL,
    last_exported_at TIMESTAMP NULL,
    emailed_by CHAR(36) NULL,
    emailed_at TIMESTAMP NULL,
    CONSTRAINT fk_contract_documents_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT uk_contract_documents_number
        UNIQUE (document_number)
) ENGINE=InnoDB;

CREATE INDEX idx_contract_documents_contract_generated_at
ON contract_documents(contract_id, generated_at);

CREATE TABLE contract_tracking_events (
    id CHAR(36) PRIMARY KEY,
    contract_id CHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_status VARCHAR(30) NULL,
    title VARCHAR(255) NOT NULL,
    note VARCHAR(1000) NULL,
    expected_at TIMESTAMP NULL,
    actual_at TIMESTAMP NULL,
    tracking_number VARCHAR(100) NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_tracking_events_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id)
) ENGINE=InnoDB;

CREATE INDEX idx_contract_tracking_events_contract_actual_at
ON contract_tracking_events(contract_id, actual_at, created_at);
