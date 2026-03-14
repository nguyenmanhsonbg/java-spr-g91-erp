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
    WHERE email = 'admin@g90steel.vn'
);
