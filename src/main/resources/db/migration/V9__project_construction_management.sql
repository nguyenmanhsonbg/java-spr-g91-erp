ALTER TABLE projects
ADD COLUMN scope VARCHAR(1000) NULL,
ADD COLUMN assigned_project_manager VARCHAR(255) NULL,
ADD COLUMN backup_warehouse_id CHAR(36) NULL,
ADD COLUMN linked_contract_id CHAR(36) NULL,
ADD COLUMN linked_order_reference VARCHAR(100) NULL,
ADD COLUMN created_by CHAR(36) NULL,
ADD COLUMN updated_by CHAR(36) NULL,
ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
ADD COLUMN progress_percent INT NOT NULL DEFAULT 0,
ADD COLUMN progress_status VARCHAR(30) NOT NULL DEFAULT 'ON_TRACK',
ADD COLUMN current_phase VARCHAR(50) NULL,
ADD COLUMN last_progress_update_at TIMESTAMP NULL,
ADD COLUMN last_progress_note VARCHAR(1000) NULL,
ADD COLUMN actual_spend DECIMAL(18,2) NOT NULL DEFAULT 0,
ADD COLUMN commitments DECIMAL(18,2) NOT NULL DEFAULT 0,
ADD COLUMN payments_received DECIMAL(18,2) NOT NULL DEFAULT 0,
ADD COLUMN payments_due DECIMAL(18,2) NOT NULL DEFAULT 0,
ADD COLUMN outstanding_balance DECIMAL(18,2) NOT NULL DEFAULT 0,
ADD COLUMN open_order_count INT NOT NULL DEFAULT 0,
ADD COLUMN unresolved_issue_count INT NOT NULL DEFAULT 0,
ADD COLUMN budget_approval_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
ADD COLUMN archive_approval_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
ADD COLUMN archived_at TIMESTAMP NULL,
ADD COLUMN archived_by CHAR(36) NULL,
ADD COLUMN archive_reason VARCHAR(1000) NULL,
ADD COLUMN restore_deadline TIMESTAMP NULL,
ADD COLUMN closed_at TIMESTAMP NULL,
ADD COLUMN closed_by CHAR(36) NULL,
ADD COLUMN close_reason VARCHAR(1000) NULL,
ADD COLUMN customer_signoff_completed BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN customer_signoff_at TIMESTAMP NULL,
ADD COLUMN customer_satisfaction_score INT NULL,
ADD COLUMN warranty_start_date DATE NULL,
ADD COLUMN warranty_end_date DATE NULL;

ALTER TABLE projects
ADD CONSTRAINT uk_projects_project_code UNIQUE (project_code),
ADD CONSTRAINT fk_project_backup_warehouse
    FOREIGN KEY (backup_warehouse_id) REFERENCES warehouses(id),
ADD CONSTRAINT fk_project_linked_contract
    FOREIGN KEY (linked_contract_id) REFERENCES contracts(id);

ALTER TABLE project_milestones
ADD COLUMN milestone_type VARCHAR(30) NOT NULL DEFAULT 'PAYMENT',
ADD COLUMN amount DECIMAL(18,2) NULL,
ADD COLUMN due_date DATE NULL,
ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
ADD COLUMN notes VARCHAR(1000) NULL,
ADD COLUMN completed_at TIMESTAMP NULL,
ADD COLUMN confirmation_deadline TIMESTAMP NULL,
ADD COLUMN confirmed_by_customer_id CHAR(36) NULL,
ADD COLUMN confirmation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
ADD COLUMN payment_release_ready BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE project_milestones
ADD CONSTRAINT fk_project_milestone_customer_confirm
    FOREIGN KEY (confirmed_by_customer_id) REFERENCES customers(id);

CREATE TABLE project_progress_updates (
    id CHAR(36) PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    previous_progress_percent INT NOT NULL DEFAULT 0,
    progress_percent INT NOT NULL,
    progress_status VARCHAR(30) NOT NULL,
    phase VARCHAR(50),
    notes VARCHAR(1000),
    change_reason VARCHAR(1000),
    behind_schedule BOOLEAN NOT NULL DEFAULT FALSE,
    evidence_count INT NOT NULL DEFAULT 0,
    created_by CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_progress_project
        FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB;

CREATE TABLE project_warehouse_assignments (
    id CHAR(36) PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    assignment_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_reason VARCHAR(1000),
    assigned_by CHAR(36),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    CONSTRAINT fk_project_warehouse_assignment_project
        FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_project_warehouse_assignment_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB;

CREATE TABLE project_documents (
    id CHAR(36) PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    progress_update_id CHAR(36) NULL,
    document_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    uploaded_by CHAR(36),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_document_project
        FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_project_document_progress
        FOREIGN KEY (progress_update_id) REFERENCES project_progress_updates(id)
) ENGINE=InnoDB;

CREATE INDEX idx_projects_customer_status ON projects(customer_id, status);
CREATE INDEX idx_projects_progress_status ON projects(progress_status);
CREATE INDEX idx_projects_archived_at ON projects(archived_at);
CREATE INDEX idx_project_progress_updates_project_created ON project_progress_updates(project_id, created_at);
CREATE INDEX idx_project_warehouse_assignments_project_active ON project_warehouse_assignments(project_id, active);
CREATE INDEX idx_project_documents_project ON project_documents(project_id);
