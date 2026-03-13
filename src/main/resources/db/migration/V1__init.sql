-- =========================================================
-- G90 Steel Business Management System
-- DATABASE INITIALIZATION
-- File: 01_init.sql
-- Target: MySQL 8+
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- DATABASE
-- =========================================================

CREATE DATABASE IF NOT EXISTS g90_steel
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE g90_steel;

-- =========================================================
-- ROLES
-- =========================================================

CREATE TABLE roles (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(50) UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- =========================================================
-- USERS
-- =========================================================

CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,

    role_id CHAR(36),

    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),

    phone VARCHAR(50),
    address VARCHAR(500),

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

-- =========================================================
-- CUSTOMERS
-- =========================================================

CREATE TABLE customers (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36),

    company_name VARCHAR(255),
    tax_code VARCHAR(20) UNIQUE,

    address VARCHAR(500),

    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),

    customer_type VARCHAR(50),

    credit_limit DECIMAL(18,2) DEFAULT 0,

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- =========================================================
-- PRODUCTS
-- =========================================================

CREATE TABLE products (
    id CHAR(36) PRIMARY KEY,

    product_code VARCHAR(50) UNIQUE,
    product_name VARCHAR(255),

    type VARCHAR(100),
    size VARCHAR(100),
    thickness VARCHAR(50),

    unit VARCHAR(20),

    weight_conversion DECIMAL(10,4),
    reference_weight DECIMAL(10,4),

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================================================
-- PRICE LIST
-- =========================================================

CREATE TABLE price_lists (
    id CHAR(36) PRIMARY KEY,

    name VARCHAR(255),

    customer_group VARCHAR(50),

    start_date DATE,
    end_date DATE,

    status VARCHAR(20),

    created_by CHAR(36),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE price_list_items (
    id CHAR(36) PRIMARY KEY,

    price_list_id CHAR(36),
    product_id CHAR(36),

    unit_price DECIMAL(18,2),

    CONSTRAINT fk_price_list
        FOREIGN KEY (price_list_id) REFERENCES price_lists(id),

    CONSTRAINT fk_price_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- =========================================================
-- PROMOTIONS
-- =========================================================

CREATE TABLE promotions (
    id CHAR(36) PRIMARY KEY,

    name VARCHAR(255),

    promotion_type VARCHAR(50),

    discount_value DECIMAL(10,2),

    start_date DATE,
    end_date DATE,

    status VARCHAR(20),

    created_by CHAR(36)
) ENGINE=InnoDB;

CREATE TABLE promotion_products (
    id CHAR(36) PRIMARY KEY,

    promotion_id CHAR(36),
    product_id CHAR(36),

    CONSTRAINT fk_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id),

    CONSTRAINT fk_promotion_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- =========================================================
-- WAREHOUSES
-- =========================================================

CREATE TABLE warehouses (
    id CHAR(36) PRIMARY KEY,

    name VARCHAR(255),
    location VARCHAR(500)
) ENGINE=InnoDB;

-- =========================================================
-- INVENTORY
-- =========================================================

CREATE TABLE inventory (
    id CHAR(36) PRIMARY KEY,

    warehouse_id CHAR(36),
    product_id CHAR(36),

    quantity DECIMAL(18,2) DEFAULT 0,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),

    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE inventory_transactions (
    id CHAR(36) PRIMARY KEY,

    product_id CHAR(36),
    warehouse_id CHAR(36),

    transaction_type VARCHAR(50),

    quantity DECIMAL(18,2),

    reference_type VARCHAR(50),
    reference_id CHAR(36),

    reason VARCHAR(255),

    created_by CHAR(36),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inv_tx_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT fk_inv_tx_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB;

-- =========================================================
-- QUOTATIONS
-- =========================================================

CREATE TABLE quotations (
    id CHAR(36) PRIMARY KEY,

    quotation_number VARCHAR(50),

    customer_id CHAR(36),
    project_id CHAR(36),

    total_amount DECIMAL(18,2),

    status VARCHAR(50),

    valid_until DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quotation_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE quotation_items (
    id CHAR(36) PRIMARY KEY,

    quotation_id CHAR(36),
    product_id CHAR(36),

    quantity DECIMAL(18,2),
    unit_price DECIMAL(18,2),
    total_price DECIMAL(18,2),

    CONSTRAINT fk_quotation_item
        FOREIGN KEY (quotation_id) REFERENCES quotations(id),

    CONSTRAINT fk_quotation_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- =========================================================
-- CONTRACTS
-- =========================================================

CREATE TABLE contracts (
    id CHAR(36) PRIMARY KEY,

    contract_number VARCHAR(50),

    customer_id CHAR(36),
    quotation_id CHAR(36),

    total_amount DECIMAL(18,2),

    status VARCHAR(50),

    payment_terms VARCHAR(255),

    delivery_address VARCHAR(500),

    created_by CHAR(36),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_contract_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),

    CONSTRAINT fk_contract_quotation
        FOREIGN KEY (quotation_id) REFERENCES quotations(id)
) ENGINE=InnoDB;

CREATE TABLE contract_items (
    id CHAR(36) PRIMARY KEY,

    contract_id CHAR(36),
    product_id CHAR(36),

    quantity DECIMAL(18,2),
    unit_price DECIMAL(18,2),
    total_price DECIMAL(18,2),

    CONSTRAINT fk_contract_item
        FOREIGN KEY (contract_id) REFERENCES contracts(id),

    CONSTRAINT fk_contract_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- =========================================================
-- PROJECTS
-- =========================================================

CREATE TABLE projects (
    id CHAR(36) PRIMARY KEY,

    project_code VARCHAR(50),

    customer_id CHAR(36),

    name VARCHAR(255),

    location VARCHAR(500),

    budget DECIMAL(18,2),

    start_date DATE,
    end_date DATE,

    status VARCHAR(20),

    warehouse_id CHAR(36),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_project_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),

    CONSTRAINT fk_project_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB;

CREATE TABLE project_milestones (
    id CHAR(36) PRIMARY KEY,

    project_id CHAR(36),

    name VARCHAR(255),

    completion_percent INT,

    confirmed BOOLEAN DEFAULT FALSE,

    confirmed_at TIMESTAMP NULL,

    CONSTRAINT fk_project_milestone
        FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB;

-- =========================================================
-- INVOICES
-- =========================================================

CREATE TABLE invoices (
    id CHAR(36) PRIMARY KEY,

    invoice_number VARCHAR(50),

    contract_id CHAR(36),
    customer_id CHAR(36),

    total_amount DECIMAL(18,2),
    vat_amount DECIMAL(18,2),

    status VARCHAR(20),

    due_date DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoice_contract
        FOREIGN KEY (contract_id) REFERENCES contracts(id),

    CONSTRAINT fk_invoice_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE invoice_items (
    id CHAR(36) PRIMARY KEY,

    invoice_id CHAR(36),
    product_id CHAR(36),

    quantity DECIMAL(18,2),
    unit_price DECIMAL(18,2),
    total_price DECIMAL(18,2),

    CONSTRAINT fk_invoice_item
        FOREIGN KEY (invoice_id) REFERENCES invoices(id),

    CONSTRAINT fk_invoice_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- =========================================================
-- PAYMENTS
-- =========================================================

CREATE TABLE payments (
    id CHAR(36) PRIMARY KEY,

    customer_id CHAR(36),

    amount DECIMAL(18,2),

    payment_method VARCHAR(50),

    reference_no VARCHAR(100),

    payment_date DATE,

    created_by CHAR(36),

    CONSTRAINT fk_payment_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;

CREATE TABLE payment_allocations (
    id CHAR(36) PRIMARY KEY,

    payment_id CHAR(36),
    invoice_id CHAR(36),

    amount DECIMAL(18,2),

    CONSTRAINT fk_payment_alloc_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id),

    CONSTRAINT fk_payment_alloc_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id)
) ENGINE=InnoDB;

-- =========================================================
-- AUDIT LOGS
-- =========================================================

CREATE TABLE audit_logs (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36),

    action VARCHAR(100),

    entity_type VARCHAR(50),

    entity_id CHAR(36),

    old_value JSON,
    new_value JSON,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
