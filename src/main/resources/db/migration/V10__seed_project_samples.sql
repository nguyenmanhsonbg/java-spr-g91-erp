-- Seed sample data for Project module.
-- This migration prefers customers/users that were seeded earlier,
-- but still works if equivalent active records already exist.

INSERT INTO warehouses (
    id,
    name,
    location
)
SELECT
    'a1111111-1111-1111-1111-111111111111',
    'Kho Tong Binh Duong',
    'KCN VSIP 1, Binh Duong'
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM warehouses
    WHERE id = 'a1111111-1111-1111-1111-111111111111'
       OR name = 'Kho Tong Binh Duong'
);

INSERT INTO warehouses (
    id,
    name,
    location
)
SELECT
    'b2222222-2222-2222-2222-222222222222',
    'Kho Du Phong Dong Nai',
    'KCN Bien Hoa 2, Dong Nai'
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM warehouses
    WHERE id = 'b2222222-2222-2222-2222-222222222222'
       OR name = 'Kho Du Phong Dong Nai'
);

SET @seed_customer_id_1 = (
    SELECT c.id
    FROM customers c
    LEFT JOIN users u ON u.id = c.user_id
    WHERE UPPER(COALESCE(c.status, 'ACTIVE')) = 'ACTIVE'
    ORDER BY
        (LOWER(COALESCE(u.email, '')) = 'customer@g90steel.vn') DESC,
        c.created_at ASC,
        c.id ASC
    LIMIT 1
);

SET @seed_customer_id_2 = COALESCE(
    (
        SELECT c.id
        FROM customers c
        LEFT JOIN users u ON u.id = c.user_id
        WHERE UPPER(COALESCE(c.status, 'ACTIVE')) = 'ACTIVE'
        ORDER BY
            (LOWER(COALESCE(u.email, '')) = 'customer@g90steel.vn') DESC,
            c.created_at ASC,
            c.id ASC
        LIMIT 1 OFFSET 1
    ),
    @seed_customer_id_1
);

SET @seed_accountant_user_id = (
    SELECT u.id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE UPPER(r.name) IN ('ACCOUNTANT', 'OWNER')
      AND UPPER(COALESCE(u.status, 'ACTIVE')) = 'ACTIVE'
    ORDER BY
        (LOWER(u.email) = 'accountant@g90steel.vn') DESC,
        (UPPER(r.name) = 'ACCOUNTANT') DESC,
        u.created_at ASC,
        u.id ASC
    LIMIT 1
);

INSERT INTO projects (
    id,
    project_code,
    customer_id,
    name,
    location,
    scope,
    budget,
    start_date,
    end_date,
    status,
    warehouse_id,
    backup_warehouse_id,
    assigned_project_manager,
    progress_percent,
    progress_status,
    current_phase,
    last_progress_update_at,
    last_progress_note,
    actual_spend,
    commitments,
    payments_received,
    payments_due,
    outstanding_balance,
    open_order_count,
    unresolved_issue_count,
    budget_approval_status,
    archive_approval_status,
    created_by,
    updated_by,
    created_at,
    updated_at,
    customer_signoff_completed
)
SELECT
    'c1111111-1111-1111-1111-111111111111',
    'PRJ-2026-9001',
    @seed_customer_id_1,
    'Du An Nha Xuong Binh Duong',
    'KCN VSIP 1, Binh Duong',
    'Thi cong ket cau thep, mai ton va khu phu tro cho nha xuong cong nghiep',
    3500000000.00,
    '2026-01-10',
    '2026-06-30',
    'ACTIVE',
    'a1111111-1111-1111-1111-111111111111',
    'b2222222-2222-2222-2222-222222222222',
    'Tran Minh Quan',
    45,
    'ON_TRACK',
    'STEEL_ERECTION',
    '2026-03-10 09:00:00',
    'Hoan thanh lap dung khung thep khu A, dang chuan bi thi cong khu B',
    1250000000.00,
    1500000000.00,
    1000000000.00,
    1800000000.00,
    800000000.00,
    1,
    0,
    'NOT_REQUIRED',
    'NOT_REQUIRED',
    @seed_accountant_user_id,
    @seed_accountant_user_id,
    '2026-01-05 08:30:00',
    '2026-03-10 09:00:00',
    FALSE
FROM dual
WHERE @seed_customer_id_1 IS NOT NULL
  AND @seed_accountant_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM projects
      WHERE project_code = 'PRJ-2026-9001'
  );

INSERT INTO projects (
    id,
    project_code,
    customer_id,
    name,
    location,
    scope,
    budget,
    start_date,
    end_date,
    status,
    warehouse_id,
    backup_warehouse_id,
    assigned_project_manager,
    progress_percent,
    progress_status,
    current_phase,
    last_progress_update_at,
    last_progress_note,
    actual_spend,
    commitments,
    payments_received,
    payments_due,
    outstanding_balance,
    open_order_count,
    unresolved_issue_count,
    budget_approval_status,
    archive_approval_status,
    created_by,
    updated_by,
    created_at,
    updated_at,
    customer_signoff_completed,
    customer_signoff_at,
    customer_satisfaction_score
)
SELECT
    'c2222222-2222-2222-2222-222222222222',
    'PRJ-2026-9002',
    @seed_customer_id_2,
    'Du An Kho Lanh Dong Nai',
    'KCN Bien Hoa 2, Dong Nai',
    'Thi cong kho lanh ket cau thep tien che, bao gom khu van hanh va san xuat',
    2200000000.00,
    '2025-11-15',
    '2026-02-28',
    'COMPLETED',
    'a1111111-1111-1111-1111-111111111111',
    NULL,
    'Le Hoang Phuc',
    100,
    'COMPLETED',
    'HANDOVER',
    '2026-02-25 16:30:00',
    'Da hoan tat nghiem thu noi bo va san sang dong du an',
    1980000000.00,
    0.00,
    2200000000.00,
    2200000000.00,
    0.00,
    0,
    0,
    'NOT_REQUIRED',
    'NOT_REQUIRED',
    @seed_accountant_user_id,
    @seed_accountant_user_id,
    '2025-11-10 09:00:00',
    '2026-02-27 10:00:00',
    TRUE,
    '2026-02-27 10:00:00',
    5
FROM dual
WHERE @seed_customer_id_2 IS NOT NULL
  AND @seed_accountant_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM projects
      WHERE project_code = 'PRJ-2026-9002'
  );

INSERT INTO project_milestones (
    id,
    project_id,
    name,
    completion_percent,
    confirmed,
    confirmed_at,
    milestone_type,
    amount,
    due_date,
    status,
    notes,
    completed_at,
    confirmation_deadline,
    confirmed_by_customer_id,
    confirmation_status,
    payment_release_ready,
    created_at,
    updated_at
)
SELECT
    'd1111111-1111-1111-1111-111111111111',
    'c1111111-1111-1111-1111-111111111111',
    'Tam Ung Dot 1',
    30,
    TRUE,
    '2026-02-16 10:15:00',
    'PAYMENT',
    1050000000.00,
    '2026-02-15',
    'CONFIRMED',
    'Da xac nhan hoan thanh phan mong va cot thep khu A',
    '2026-02-14 15:00:00',
    '2026-02-21 15:00:00',
    @seed_customer_id_1,
    'CONFIRMED',
    TRUE,
    '2026-01-05 08:30:00',
    '2026-02-16 10:15:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_milestones WHERE id = 'd1111111-1111-1111-1111-111111111111'
);

INSERT INTO project_milestones (
    id,
    project_id,
    name,
    completion_percent,
    confirmed,
    milestone_type,
    amount,
    due_date,
    status,
    notes,
    confirmation_status,
    payment_release_ready,
    created_at,
    updated_at
)
SELECT
    'd1111111-1111-1111-1111-111111111112',
    'c1111111-1111-1111-1111-111111111111',
    'Tam Ung Dot 2',
    60,
    FALSE,
    'PAYMENT',
    1400000000.00,
    '2026-04-15',
    'PENDING',
    'Cho hoan thanh lap dung khung thep khu B',
    'PENDING',
    FALSE,
    '2026-01-05 08:30:00',
    '2026-03-10 09:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_milestones WHERE id = 'd1111111-1111-1111-1111-111111111112'
);

INSERT INTO project_milestones (
    id,
    project_id,
    name,
    completion_percent,
    confirmed,
    milestone_type,
    amount,
    due_date,
    status,
    notes,
    confirmation_status,
    payment_release_ready,
    created_at,
    updated_at
)
SELECT
    'd1111111-1111-1111-1111-111111111113',
    'c1111111-1111-1111-1111-111111111111',
    'Quyet Toan Du An',
    100,
    FALSE,
    'PAYMENT',
    1050000000.00,
    '2026-06-25',
    'PENDING',
    'Cho nghiem thu va ban giao hoan tat',
    'PENDING',
    FALSE,
    '2026-01-05 08:30:00',
    '2026-03-10 09:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_milestones WHERE id = 'd1111111-1111-1111-1111-111111111113'
);

INSERT INTO project_milestones (
    id,
    project_id,
    name,
    completion_percent,
    confirmed,
    confirmed_at,
    milestone_type,
    amount,
    due_date,
    status,
    notes,
    completed_at,
    confirmation_deadline,
    confirmed_by_customer_id,
    confirmation_status,
    payment_release_ready,
    created_at,
    updated_at
)
SELECT
    'd2222222-2222-2222-2222-222222222221',
    'c2222222-2222-2222-2222-222222222222',
    'Tam Ung Thi Cong',
    35,
    TRUE,
    '2025-12-20 11:00:00',
    'PAYMENT',
    770000000.00,
    '2025-12-18',
    'CONFIRMED',
    'Da xac nhan xong phan mong va lap dung khung chinh',
    '2025-12-18 16:00:00',
    '2025-12-25 16:00:00',
    @seed_customer_id_2,
    'CONFIRMED',
    TRUE,
    '2025-11-10 09:00:00',
    '2025-12-20 11:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c2222222-2222-2222-2222-222222222222'
)
AND NOT EXISTS (
    SELECT 1 FROM project_milestones WHERE id = 'd2222222-2222-2222-2222-222222222221'
);

INSERT INTO project_milestones (
    id,
    project_id,
    name,
    completion_percent,
    confirmed,
    confirmed_at,
    milestone_type,
    amount,
    due_date,
    status,
    notes,
    completed_at,
    confirmation_deadline,
    confirmed_by_customer_id,
    confirmation_status,
    payment_release_ready,
    created_at,
    updated_at
)
SELECT
    'd2222222-2222-2222-2222-222222222222',
    'c2222222-2222-2222-2222-222222222222',
    'Thanh Toan Gan Hoan Thanh',
    70,
    TRUE,
    '2026-01-30 14:30:00',
    'PAYMENT',
    770000000.00,
    '2026-01-28',
    'CONFIRMED',
    'Da hoan thanh 70 phan tram khoi luong va duoc customer xac nhan',
    '2026-01-28 15:00:00',
    '2026-02-04 15:00:00',
    @seed_customer_id_2,
    'CONFIRMED',
    TRUE,
    '2025-11-10 09:00:00',
    '2026-01-30 14:30:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c2222222-2222-2222-2222-222222222222'
)
AND NOT EXISTS (
    SELECT 1 FROM project_milestones WHERE id = 'd2222222-2222-2222-2222-222222222222'
);

INSERT INTO project_milestones (
    id,
    project_id,
    name,
    completion_percent,
    confirmed,
    confirmed_at,
    milestone_type,
    amount,
    due_date,
    status,
    notes,
    completed_at,
    confirmation_deadline,
    confirmed_by_customer_id,
    confirmation_status,
    payment_release_ready,
    created_at,
    updated_at
)
SELECT
    'd2222222-2222-2222-2222-222222222223',
    'c2222222-2222-2222-2222-222222222222',
    'Quyet Toan Hoan Tat',
    100,
    TRUE,
    '2026-02-27 10:00:00',
    'PAYMENT',
    660000000.00,
    '2026-02-27',
    'CONFIRMED',
    'Customer da nghiem thu va xac nhan du dieu kien dong du an',
    '2026-02-25 16:30:00',
    '2026-03-04 16:30:00',
    @seed_customer_id_2,
    'CONFIRMED',
    TRUE,
    '2025-11-10 09:00:00',
    '2026-02-27 10:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c2222222-2222-2222-2222-222222222222'
)
AND NOT EXISTS (
    SELECT 1 FROM project_milestones WHERE id = 'd2222222-2222-2222-2222-222222222223'
);

INSERT INTO project_progress_updates (
    id,
    project_id,
    previous_progress_percent,
    progress_percent,
    progress_status,
    phase,
    notes,
    change_reason,
    behind_schedule,
    evidence_count,
    created_by,
    created_at,
    updated_at
)
SELECT
    'e1111111-1111-1111-1111-111111111111',
    'c1111111-1111-1111-1111-111111111111',
    0,
    20,
    'ON_TRACK',
    'FOUNDATION',
    'Hoan thanh phan mong va bo tri bu long neo',
    'Cap nhat tien do dau ky',
    FALSE,
    1,
    @seed_accountant_user_id,
    '2026-01-25 10:00:00',
    '2026-01-25 10:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_progress_updates WHERE id = 'e1111111-1111-1111-1111-111111111111'
);

INSERT INTO project_progress_updates (
    id,
    project_id,
    previous_progress_percent,
    progress_percent,
    progress_status,
    phase,
    notes,
    change_reason,
    behind_schedule,
    evidence_count,
    created_by,
    created_at,
    updated_at
)
SELECT
    'e1111111-1111-1111-1111-111111111112',
    'c1111111-1111-1111-1111-111111111111',
    20,
    45,
    'ON_TRACK',
    'STEEL_ERECTION',
    'Da lap dung khung thep khu A va hoan thanh mot phan khu B',
    'Cap nhat tien do tuan 10',
    FALSE,
    2,
    @seed_accountant_user_id,
    '2026-03-10 09:00:00',
    '2026-03-10 09:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_progress_updates WHERE id = 'e1111111-1111-1111-1111-111111111112'
);

INSERT INTO project_progress_updates (
    id,
    project_id,
    previous_progress_percent,
    progress_percent,
    progress_status,
    phase,
    notes,
    change_reason,
    behind_schedule,
    evidence_count,
    created_by,
    created_at,
    updated_at
)
SELECT
    'e2222222-2222-2222-2222-222222222221',
    'c2222222-2222-2222-2222-222222222222',
    70,
    100,
    'COMPLETED',
    'HANDOVER',
    'Hoan tat nghiem thu, ban giao va day du ho so hoan cong',
    'Cap nhat ket thuc du an',
    FALSE,
    1,
    @seed_accountant_user_id,
    '2026-02-25 16:30:00',
    '2026-02-25 16:30:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c2222222-2222-2222-2222-222222222222'
)
AND NOT EXISTS (
    SELECT 1 FROM project_progress_updates WHERE id = 'e2222222-2222-2222-2222-222222222221'
);

INSERT INTO project_warehouse_assignments (
    id,
    project_id,
    warehouse_id,
    assignment_type,
    active,
    assignment_reason,
    assigned_by,
    assigned_at
)
SELECT
    'f1111111-1111-1111-1111-111111111111',
    'c1111111-1111-1111-1111-111111111111',
    'a1111111-1111-1111-1111-111111111111',
    'PRIMARY',
    TRUE,
    'Gan kho tong phuc vu cap vat tu chinh cho du an',
    @seed_accountant_user_id,
    '2026-01-05 09:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_warehouse_assignments WHERE id = 'f1111111-1111-1111-1111-111111111111'
);

INSERT INTO project_warehouse_assignments (
    id,
    project_id,
    warehouse_id,
    assignment_type,
    active,
    assignment_reason,
    assigned_by,
    assigned_at
)
SELECT
    'f1111111-1111-1111-1111-111111111112',
    'c1111111-1111-1111-1111-111111111111',
    'b2222222-2222-2222-2222-222222222222',
    'BACKUP',
    TRUE,
    'Kho backup cho truong hop tang tai giao nhan',
    @seed_accountant_user_id,
    '2026-01-05 09:05:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c1111111-1111-1111-1111-111111111111'
)
AND NOT EXISTS (
    SELECT 1 FROM project_warehouse_assignments WHERE id = 'f1111111-1111-1111-1111-111111111112'
);

INSERT INTO project_warehouse_assignments (
    id,
    project_id,
    warehouse_id,
    assignment_type,
    active,
    assignment_reason,
    assigned_by,
    assigned_at
)
SELECT
    'f2222222-2222-2222-2222-222222222221',
    'c2222222-2222-2222-2222-222222222222',
    'a1111111-1111-1111-1111-111111111111',
    'PRIMARY',
    TRUE,
    'Kho chinh phuc vu giao hang xuyen suot cho kho lanh',
    @seed_accountant_user_id,
    '2025-11-10 09:10:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM projects WHERE id = 'c2222222-2222-2222-2222-222222222222'
)
AND NOT EXISTS (
    SELECT 1 FROM project_warehouse_assignments WHERE id = 'f2222222-2222-2222-2222-222222222221'
);

INSERT INTO project_documents (
    id,
    project_id,
    progress_update_id,
    document_type,
    file_name,
    file_url,
    content_type,
    uploaded_by,
    uploaded_at
)
SELECT
    'a3333333-3333-3333-3333-333333333331',
    'c1111111-1111-1111-1111-111111111111',
    'e1111111-1111-1111-1111-111111111112',
    'PHOTO',
    'lap-dung-khung-thep-khu-a.jpg',
    'https://example.com/project/c1111111/photos/lap-dung-khung-thep-khu-a.jpg',
    'image/jpeg',
    @seed_accountant_user_id,
    '2026-03-10 09:05:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM project_progress_updates WHERE id = 'e1111111-1111-1111-1111-111111111112'
)
AND NOT EXISTS (
    SELECT 1 FROM project_documents WHERE id = 'a3333333-3333-3333-3333-333333333331'
);

INSERT INTO project_documents (
    id,
    project_id,
    progress_update_id,
    document_type,
    file_name,
    file_url,
    content_type,
    uploaded_by,
    uploaded_at
)
SELECT
    'a3333333-3333-3333-3333-333333333332',
    'c2222222-2222-2222-2222-222222222222',
    'e2222222-2222-2222-2222-222222222221',
    'DOCUMENT',
    'bien-ban-nghiem-thu-hoan-thanh.pdf',
    'https://example.com/project/c2222222/docs/bien-ban-nghiem-thu-hoan-thanh.pdf',
    'application/pdf',
    @seed_accountant_user_id,
    '2026-02-25 17:00:00'
FROM dual
WHERE EXISTS (
    SELECT 1 FROM project_progress_updates WHERE id = 'e2222222-2222-2222-2222-222222222221'
)
AND NOT EXISTS (
    SELECT 1 FROM project_documents WHERE id = 'a3333333-3333-3333-3333-333333333332'
);
