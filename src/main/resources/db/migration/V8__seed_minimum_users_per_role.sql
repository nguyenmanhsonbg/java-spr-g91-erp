-- Ensure each system role has at least one default active user.
-- Default password for all seeded users: admin

INSERT INTO users (
    id,
    role_id,
    full_name,
    email,
    password_hash,
    phone,
    address,
    status,
    created_at,
    updated_at
)
SELECT
    '55555555-5555-5555-5555-555555555555',
    '11111111-1111-1111-1111-111111111111',
    'System Admin',
    'admin@g90steel.vn',
    '$2a$10$lHF8qcAsHvDfBToQiKQcuemYo5NjbQ1eKV.N56XKhZwAHjfX/MWXu',
    '0900000000',
    'Head Office',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE role_id = '11111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'admin@g90steel.vn'
);

INSERT INTO users (
    id,
    role_id,
    full_name,
    email,
    password_hash,
    phone,
    address,
    status,
    created_at,
    updated_at
)
SELECT
    '66666666-6666-6666-6666-666666666666',
    '22222222-2222-2222-2222-222222222222',
    'Accountant Default',
    'accountant@g90steel.vn',
    '$2a$10$lHF8qcAsHvDfBToQiKQcuemYo5NjbQ1eKV.N56XKhZwAHjfX/MWXu',
    '0900000001',
    'Accounting Office',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE role_id = '22222222-2222-2222-2222-222222222222'
)
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'accountant@g90steel.vn'
);

INSERT INTO users (
    id,
    role_id,
    full_name,
    email,
    password_hash,
    phone,
    address,
    status,
    created_at,
    updated_at
)
SELECT
    '77777777-7777-7777-7777-777777777777',
    '33333333-3333-3333-3333-333333333333',
    'Warehouse Default',
    'warehouse@g90steel.vn',
    '$2a$10$lHF8qcAsHvDfBToQiKQcuemYo5NjbQ1eKV.N56XKhZwAHjfX/MWXu',
    '0900000002',
    'Warehouse Zone A',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE role_id = '33333333-3333-3333-3333-333333333333'
)
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'warehouse@g90steel.vn'
);

INSERT INTO users (
    id,
    role_id,
    full_name,
    email,
    password_hash,
    phone,
    address,
    status,
    created_at,
    updated_at
)
SELECT
    '88888888-8888-8888-8888-888888888888',
    '44444444-4444-4444-4444-444444444444',
    'Customer Default',
    'customer@g90steel.vn',
    '$2a$10$lHF8qcAsHvDfBToQiKQcuemYo5NjbQ1eKV.N56XKhZwAHjfX/MWXu',
    '0900000003',
    'Customer Office',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE role_id = '44444444-4444-4444-4444-444444444444'
)
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'customer@g90steel.vn'
);

INSERT INTO customers (
    id,
    user_id,
    company_name,
    tax_code,
    address,
    contact_person,
    phone,
    email,
    customer_type,
    credit_limit,
    status,
    created_at
)
SELECT
    '99999999-9999-9999-9999-999999999999',
    u.id,
    'Cong Ty TNHH Khach Hang Mac Dinh',
    '0109998888',
    'Ha Noi',
    'Customer Default',
    '0900000003',
    'customer@g90steel.vn',
    'CONTRACTOR',
    500000000.00,
    'ACTIVE',
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'customer@g90steel.vn'
AND NOT EXISTS (
    SELECT 1
    FROM customers c
    WHERE c.user_id = u.id
);
