# Đặc tả hoàn thiện luồng Đăng ký, Đăng nhập và Phân quyền

## 1. Thông tin tài liệu

- **Tài liệu:** Authentication, Registration & Authorization Completion Spec
- **Project:** G90 Steel Business Management System
- **Phạm vi:** Hoàn thiện đặc tả cho các luồng:
  - Đăng ký tài khoản Customer
  - Đăng nhập hệ thống
  - Đăng xuất
  - Phân quyền truy cập theo role
  - Quản lý phiên xác thực cơ bản
  - Reset / đổi mật khẩu ở mức cần thiết để hoàn chỉnh luồng auth
- **Nguồn tham chiếu:** Project Spec (`Spec_Project_G90.md`), Database Schema (`V1__init.sql`)
- **Mục tiêu:** Biến phần mô tả nghiệp vụ hiện có thành đặc tả đủ chi tiết để thiết kế API, backend security, database bổ sung và kiểm thử

---

## 2. Mục tiêu nghiệp vụ

Hệ thống cần có một cơ chế xác thực và phân quyền thống nhất để:

1. Cho phép **Guest** đăng ký tài khoản mới với vai trò **Customer**
2. Cho phép **Owner / Accountant / Warehouse / Customer** đăng nhập an toàn
3. Chặn tài khoản không hợp lệ, inactive hoặc locked
4. Cấp quyền truy cập đúng theo **role**
5. Cho phép người dùng xem và cập nhật thông tin cá nhân của chính mình
6. Hỗ trợ đổi mật khẩu và quên mật khẩu
7. Làm nền tảng bảo mật cho toàn bộ module nghiệp vụ khác

---

## 3. Phạm vi tài liệu

## 3.1 Trong phạm vi
- Register
- Login
- Logout
- Change Password
- Forgot Password / Reset Password
- View My Profile
- Update My Profile
- Role-based Access Control (RBAC)
- Session/JWT auth design ở mức ứng dụng
- Password hashing policy
- Authorization rules theo module / endpoint

## 3.2 Ngoài phạm vi
- SSO
- IAM ngoài hệ thống
- OAuth2 social login
- MFA / OTP
- Device management
- Fine-grained permission theo policy engine nâng cao
- Federated identity

---

## 4. Actor và vai trò

## 4.1 Actor

### Guest
- Người chưa đăng nhập
- Chỉ được truy cập chức năng public
- Có thể đăng ký tài khoản Customer

### Customer
- Người dùng đã đăng ký
- Truy cập các chức năng self-service như quotation, contract tracking, invoice/debt viewing, project viewing

### Accountant
- Người dùng nội bộ
- Truy cập các chức năng kế toán, customer, contract, invoice, debt

### Warehouse
- Người dùng nội bộ
- Truy cập các chức năng product, inventory, warehouse operations

### Owner
- Vai trò cao nhất
- Quản lý account nội bộ, pricing, promotion, dashboard, reports, approvals

---

## 5. Nguyên tắc bảo mật cốt lõi

1. Mọi truy cập ngoài public area phải yêu cầu xác thực
2. Mọi quyền truy cập nghiệp vụ được quyết định bởi role
3. Password không bao giờ lưu plain text
4. Email phải là định danh đăng nhập duy nhất
5. Tài khoản inactive/locked không được login
6. User chỉ được thao tác dữ liệu của chính mình nếu là self-service flow
7. Các hành động nhạy cảm phải có audit log
8. Hệ thống phải từ chối truy cập nếu role không phù hợp
9. Hệ thống phải tách bạch rõ authentication và authorization
10. Lỗi trả về không được làm lộ thông tin nhạy cảm

---

## 6. Mô hình xác thực đề xuất

Để phù hợp với backend web hiện đại, đặc tả đề xuất dùng:

- **JWT Bearer Authentication** cho API
- Refresh token nếu muốn mở rộng
- Hoặc session-based auth nếu hệ thống web monolith

### Khuyến nghị
Nếu hệ thống sẽ có frontend và backend tách rời:
- dùng `access token` ngắn hạn
- dùng `refresh token` dài hơn

Nếu hệ thống là web MVC thuần:
- có thể dùng server session

### Trong tài liệu này
Đặc tả mặc định theo hướng:
- `POST /api/auth/login` trả access token
- các API protected dùng `Authorization: Bearer <token>`

---

## 7. Vai trò và quyền chuẩn hóa

## 7.1 Role master

Bảng `roles.name` nên chuẩn hóa giá trị:

- `OWNER`
- `ACCOUNTANT`
- `WAREHOUSE`
- `CUSTOMER`

## 7.2 Mapping role với chức năng

| Role | Nhóm quyền chính |
|---|---|
| OWNER | account management, pricing, promotion, dashboard, reports, contract approval |
| ACCOUNTANT | customers, projects, contracts, invoices, payments, debts, selected reports |
| WAREHOUSE | products, inventory, inventory reports |
| CUSTOMER | self-service quotation, contract tracking, own invoices, own debts, own projects |
| GUEST | register, login screen, public catalog |

---

## 8. Mô hình phân quyền

## 8.1 Loại phân quyền
Hệ thống dùng **RBAC** – Role Based Access Control.

### Layer 1: Authentication
Kiểm tra:
- có token/session hợp lệ hay không

### Layer 2: Authorization by role
Kiểm tra:
- role có được phép gọi endpoint hay không

### Layer 3: Ownership / data scope
Kiểm tra:
- user có quyền truy cập đúng bản ghi dữ liệu hay không

Ví dụ:
- Customer có thể gọi `GET /api/invoices/{id}`
- nhưng chỉ khi invoice đó thuộc về customer hiện tại

---

## 9. Đặc tả hoàn thiện luồng Register

## 9.1 Mục tiêu
Cho phép Guest đăng ký tài khoản Customer mới.

## 9.2 Input theo spec gốc
- Full name
- Email
- Password
- Confirm password

## 9.3 Input đề xuất hoàn thiện
```json
{
  "fullName": "Nguyen Van A",
  "email": "customer@g90steel.vn",
  "password": "123456",
  "confirmPassword": "123456",
  "phone": "0901234567"
}
```

### Ghi chú
- `phone` có thể để optional
- nếu UI muốn đúng spec gốc tuyệt đối thì không bắt buộc phone

## 9.4 Validation
- `fullName` bắt buộc
- `email` bắt buộc
- `email` đúng format
- `email` unique trong bảng `users`
- `password` bắt buộc
- `password.length >= 6`
- `confirmPassword` bắt buộc
- `confirmPassword == password`

## 9.5 Xử lý nghiệp vụ
1. Guest submit form
2. Validate dữ liệu
3. Kiểm tra duplicate email
4. Tìm role `CUSTOMER`
5. Hash password bằng BCrypt/Argon2
6. Tạo bản ghi `users`
7. Tạo bản ghi `customers` tối giản
8. Ghi audit log
9. Trả kết quả thành công

## 9.6 Mapping DB

### Insert `users`
- `id`
- `role_id`
- `full_name`
- `email`
- `password_hash`
- `phone`
- `status = ACTIVE`
- `created_at`
- `updated_at`

### Insert `customers`
- `id`
- `user_id`
- `contact_person = fullName`
- `phone`
- `email`
- `status = ACTIVE`

## 9.7 Response đề xuất
```json
{
  "message": "Register successfully",
  "userId": "uuid-user",
  "role": "CUSTOMER"
}
```

## 9.8 Lưu ý hoàn thiện
Spec gốc chưa mô tả email verification. Có 2 lựa chọn:

### Option A – Đăng ký xong dùng ngay
- status = ACTIVE
- login được ngay

### Option B – Yêu cầu xác minh email
- status = PENDING_VERIFICATION
- chỉ login được sau verify email

**Khuyến nghị cho đồ án / MVP:** dùng **Option A** để đơn giản hóa.

---

## 10. Đặc tả hoàn thiện luồng Login

## 10.1 Mục tiêu
Cho phép user xác thực bằng email + password và truy cập hệ thống theo role.

## 10.2 Input
```json
{
  "email": "owner@g90steel.vn",
  "password": "123456"
}
```

## 10.3 Validation
- Email bắt buộc
- Password bắt buộc
- Email đúng format cơ bản
- Tìm thấy user theo email
- Password đúng
- Status của user cho phép login

## 10.4 Rule status khi login
Cho phép login khi:
- `status = ACTIVE`

Từ chối login khi:
- `status = INACTIVE`
- `status = LOCKED`

## 10.5 Xử lý nghiệp vụ
1. User gửi email + password
2. System validate input
3. Query `users` theo email
4. Nếu không có user -> trả lỗi generic
5. So khớp password hash
6. Kiểm tra `status`
7. Load role từ `roles`
8. Tạo access token / session
9. Trả user profile cơ bản và quyền

## 10.6 Response đề xuất – JWT
```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid-user",
    "fullName": "Owner G90",
    "email": "owner@g90steel.vn",
    "role": "OWNER",
    "status": "ACTIVE"
  }
}
```

## 10.7 Claim đề xuất trong JWT
```json
{
  "sub": "uuid-user",
  "email": "owner@g90steel.vn",
  "role": "OWNER",
  "status": "ACTIVE"
}
```

## 10.8 Rule chống lộ thông tin
Không phân biệt lỗi:
- email không tồn tại
- password sai

Trả chung kiểu:
- “Incorrect username or password”

---

## 11. Logout

## 11.1 Mục tiêu
Kết thúc phiên xác thực.

## 11.2 Nếu dùng JWT stateless
- phía client xóa token
- backend có thể không cần lưu session
- nếu có refresh token thì revoke refresh token

## 11.3 Nếu dùng session-based
- invalidate session server-side

## 11.4 API đề xuất
`POST /api/auth/logout`

## 11.5 Response
```json
{
  "message": "Logout successfully"
}
```

---

## 12. Change Password

## 12.1 Mục tiêu
Cho phép user đã login đổi mật khẩu.

## 12.2 Input
```json
{
  "currentPassword": "123456",
  "newPassword": "654321",
  "confirmNewPassword": "654321"
}
```

## 12.3 Validation
- tất cả field bắt buộc
- current password đúng
- new password >= 6 ký tự
- new password = confirm new password
- nên chặn new password trùng current password

## 12.4 Xử lý
1. Xác thực user hiện tại từ token
2. Load `users`
3. Verify current password
4. Hash new password
5. Update `password_hash`
6. Update `updated_at`
7. Ghi audit log

---

## 13. Forgot Password / Reset Password

## 13.1 Vấn đề của schema hiện tại
Database hiện tại chưa có bảng/token để reset password hoàn chỉnh.

## 13.2 Đặc tả hoàn thiện đề xuất
Tách làm 2 bước:

### Bước 1: Request reset password
`POST /api/auth/forgot-password`

Request:
```json
{
  "email": "customer@g90steel.vn"
}
```

Hệ thống:
1. validate email
2. tìm user
3. sinh token
4. lưu token
5. gửi email link reset

### Bước 2: Confirm reset password
`POST /api/auth/reset-password`

Request:
```json
{
  "token": "reset-token",
  "newPassword": "654321",
  "confirmNewPassword": "654321"
}
```

Hệ thống:
1. validate token
2. kiểm tra chưa hết hạn, chưa used
3. hash password mới
4. update `users.password_hash`
5. đánh dấu token used
6. ghi audit log

## 13.3 Bảng cần bổ sung
```sql
CREATE TABLE password_reset_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 14. View My Profile

## 14.1 Mục tiêu
Cho phép user xem hồ sơ account của chính mình.

## 14.2 API đề xuất
`GET /api/users/me`

## 14.3 Response
```json
{
  "id": "uuid-user",
  "fullName": "Nguyen Van A",
  "email": "customer@g90steel.vn",
  "role": "CUSTOMER",
  "phone": "0901234567",
  "address": "Ha Noi",
  "status": "ACTIVE",
  "createdAt": "2026-03-14T09:00:00+07:00",
  "updatedAt": "2026-03-14T09:30:00+07:00"
}
```

---

## 15. Update My Profile

## 15.1 Mục tiêu
Cho phép user cập nhật thông tin cá nhân của chính mình.

## 15.2 API đề xuất
`PUT /api/users/me`

Request:
```json
{
  "fullName": "Nguyen Van B",
  "phone": "0908888888",
  "address": "Ho Chi Minh"
}
```

## 15.3 Rule
Cho phép sửa:
- `full_name`
- `phone`
- `address`

Không cho sửa:
- `email`
- `role`
- `status`

## 15.4 Xử lý
1. Lấy user hiện tại từ token
2. Validate field
3. Update `users`
4. Ghi audit log
5. Trả profile mới

---

## 16. Authorization matrix chuẩn hóa cho backend

Bảng dưới đây dùng để chuẩn hóa middleware/security config.

| Chức năng / API group | Guest | Customer | Warehouse | Accountant | Owner |
|---|---:|---:|---:|---:|---:|
| Register | X |  |  |  |  |
| Login | X | X | X | X | X |
| Forgot/Reset Password | X | X | X | X | X |
| View My Profile |  | X | X | X | X |
| Update My Profile |  | X | X | X | X |
| Change Password |  | X | X | X | X |
| Account Management |  |  |  |  | X |
| Product Management | X (view only) | X (view only) | X |  |  |
| Pricing Management |  |  |  | X (view only) | X |
| Promotion Management | X (view only) | X (view/view own relevance) |  | X (view) | X |
| Inventory Management |  |  | X |  |  |
| Quotation Creation |  | X |  |  |  |
| Quotation Viewing |  | X (own only) |  | X |  |
| Contract Viewing |  | X (own only) |  | X | X |
| Contract Create/Update/Submit/Cancel |  |  |  | X |  |
| Contract Approval |  |  |  |  | X |
| Customer Management |  |  |  | X |  |
| Project Management |  | X (own only view selected functions) |  | X |  |
| Invoice Viewing |  | X (own only) |  | X | X |
| Invoice Create/Update/Cancel |  |  |  | X |  |
| Debt Viewing |  | X (own only) |  | X | X |
| Payment Recording |  |  |  | X |  |
| Reports |  |  | X (inventory report only) | X | X |
| Dashboard |  |  |  |  | X |

---

## 17. Authorization rule chi tiết theo kiểu kỹ thuật

## 17.1 Rule loại 1 – Public endpoint
Không cần token:
- `/api/auth/register`
- `/api/auth/login`
- `/api/auth/forgot-password`
- `/api/auth/reset-password`
- public product catalog APIs

## 17.2 Rule loại 2 – Authenticated endpoint
Chỉ cần login:
- `/api/auth/logout`
- `/api/users/me`
- `/api/auth/change-password`

## 17.3 Rule loại 3 – Role-protected endpoint
Ví dụ:
- `/api/accounts/**` -> `OWNER`
- `/api/price-lists/**` create/update/delete -> `OWNER`
- `/api/inventory/**` -> `WAREHOUSE`
- `/api/customers/**` -> `ACCOUNTANT`
- `/api/contracts/approve/**` -> `OWNER`

## 17.4 Rule loại 4 – Ownership-protected endpoint
Ví dụ:
- Customer gọi `/api/quotations/{id}` chỉ được xem quotation có `customer_id` của chính mình
- Customer gọi `/api/invoices/{id}` chỉ được xem invoice của chính mình
- Customer gọi `/api/projects/{id}` chỉ được xem project thuộc customer của mình

---

## 18. Đặc tả ownership check

## 18.1 Customer ownership mapping
User hiện tại -> `users.id`
Từ đó map sang `customers.user_id = users.id`
Lấy được `customers.id` hiện tại

Sau đó:
- quotation phải có `quotations.customer_id = currentCustomerId`
- contract/invoice/project/debt cũng phải map cùng customer

## 18.2 Quy tắc
Nếu customer cố truy cập bản ghi không thuộc mình:
- trả `403 Forbidden` hoặc `404 Not Found` theo security policy

### Khuyến nghị
Dùng `403 Forbidden` ở API nội bộ.
Có thể dùng `404` nếu muốn giảm khả năng enumeration.

---

## 19. Đề xuất schema bổ sung để hoàn thiện auth

## 19.1 Bảng reset token
```sql
CREATE TABLE password_reset_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## 19.2 Theo dõi đăng nhập thất bại
```sql
ALTER TABLE users
ADD COLUMN failed_login_count INT DEFAULT 0,
ADD COLUMN locked_until TIMESTAMP NULL,
ADD COLUMN last_login_at TIMESTAMP NULL;
```

## 19.3 Email verification nếu cần
```sql
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN DEFAULT TRUE;
```

### Ghi chú
Nếu hệ thống MVP không dùng verify email, có thể mặc định `TRUE`.

---

## 20. Chính sách password đề xuất

Spec gốc chỉ yêu cầu tối thiểu 6 ký tự. Để hoàn thiện hơn, có thể dùng policy:

### Mức tối thiểu cho MVP
- ít nhất 6 ký tự

### Mức khuyến nghị
- tối thiểu 8 ký tự
- có chữ và số
- không trùng email
- không trùng mật khẩu cũ gần nhất

### Khuyến nghị thực tế cho đồ án
Giữ spec gốc:
- min 6
- hash bằng BCrypt

---

## 21. Mã lỗi và phản hồi chuẩn hóa

## 21.1 Nhóm lỗi xác thực
- `AUTH_001`: Missing email
- `AUTH_002`: Missing password
- `AUTH_003`: Invalid email format
- `AUTH_004`: Incorrect username or password
- `AUTH_005`: Account inactive
- `AUTH_006`: Account locked
- `AUTH_007`: Token invalid or expired
- `AUTH_008`: Current password incorrect
- `AUTH_009`: Password confirmation mismatch

## 21.2 Nhóm lỗi phân quyền
- `AUTHZ_001`: Access denied
- `AUTHZ_002`: Role not allowed
- `AUTHZ_003`: Resource does not belong to current user

## 21.3 Nhóm lỗi đăng ký
- `REG_001`: Email already exists
- `REG_002`: Missing full name
- `REG_003`: Password too short

---

## 22. API set hoàn chỉnh đề xuất

| API | Method | Endpoint | Public / Protected |
|---|---|---|---|
| Register | POST | /api/auth/register | Public |
| Login | POST | /api/auth/login | Public |
| Logout | POST | /api/auth/logout | Protected |
| Forgot Password | POST | /api/auth/forgot-password | Public |
| Reset Password | POST | /api/auth/reset-password | Public |
| Change Password | POST | /api/auth/change-password | Protected |
| View My Profile | GET | /api/users/me | Protected |
| Update My Profile | PUT | /api/users/me | Protected |

---

## 23. Security filter / middleware flow đề xuất

```text
1. Request vào hệ thống
2. Xác định endpoint có phải public không
3. Nếu protected:
   3.1 Kiểm tra token/session
   3.2 Parse principal
   3.3 Load role
   3.4 Kiểm tra role có quyền không
   3.5 Nếu là resource ownership endpoint -> kiểm tra ownership
4. Cho request đi tiếp vào business layer
```

---

## 24. Audit log đề xuất

Các action cần log:
- `REGISTER_USER`
- `LOGIN_SUCCESS`
- `LOGIN_FAILED`
- `LOGOUT`
- `CHANGE_PASSWORD`
- `REQUEST_RESET_PASSWORD`
- `RESET_PASSWORD_SUCCESS`
- `UPDATE_PROFILE`

### Ví dụ log login fail
```json
{
  "action": "LOGIN_FAILED",
  "entityType": "USER",
  "entityId": null,
  "newValue": {
    "email": "customer@g90steel.vn",
    "reason": "INVALID_CREDENTIALS"
  }
}
```

---

## 25. Acceptance criteria

Hệ thống được xem là hoàn thiện luồng đăng ký, đăng nhập, phân quyền khi:

1. Guest đăng ký được tài khoản Customer mới
2. Email duplicate bị chặn
3. Password được hash trước khi lưu
4. User active login được thành công
5. User inactive/locked không login được
6. Token/session được tạo đúng sau login
7. Logout làm mất hiệu lực phiên đăng nhập
8. User xem/cập nhật được profile của chính mình
9. User đổi mật khẩu được
10. Quên mật khẩu hoạt động được thông qua token reset
11. Mọi endpoint protected đều chặn khi chưa login
12. Endpoint role-specific chặn đúng theo role
13. Customer không xem được dữ liệu của customer khác
14. Owner/Accountant/Warehouse chỉ vào đúng nhóm chức năng được cấp
15. Các thao tác auth quan trọng được ghi log

---

## 26. Test scenarios cốt lõi

### Register
- TC01: Register thành công
- TC02: Register email trùng
- TC03: Register password mismatch
- TC04: Register thiếu required field

### Login
- TC05: Login thành công với Owner
- TC06: Login thành công với Customer
- TC07: Login sai password
- TC08: Login account inactive
- TC09: Login account locked

### Authorization
- TC10: Customer truy cập API owner -> bị chặn
- TC11: Warehouse truy cập inventory API -> cho phép
- TC12: Accountant truy cập inventory write API -> bị chặn
- TC13: Owner truy cập account management -> cho phép

### Ownership
- TC14: Customer xem invoice của chính mình -> cho phép
- TC15: Customer xem invoice của customer khác -> bị chặn
- TC16: Customer xem project của chính mình -> cho phép
- TC17: Customer xem project của customer khác -> bị chặn

### Password
- TC18: Change password thành công
- TC19: Change password sai current password
- TC20: Forgot password gửi token thành công
- TC21: Reset password bằng token hết hạn -> bị chặn

---

## 27. Kết luận và khuyến nghị triển khai

Để hoàn thiện luồng **đăng ký, đăng nhập, phân quyền** cho G90, hệ thống nên triển khai theo hướng:

1. **RBAC chuẩn hóa** với 4 role: OWNER / ACCOUNTANT / WAREHOUSE / CUSTOMER
2. **JWT auth** cho API
3. **Ownership check** cho các dữ liệu self-service của Customer
4. **BCrypt password hashing**
5. **Bổ sung bảng password reset token**
6. **Bổ sung tracking failed login / locked status** nếu muốn siết bảo mật
7. Tách rõ:
   - **Authentication**: đăng ký, đăng nhập, logout, reset password
   - **Authorization**: kiểm soát quyền endpoint
   - **Ownership**: kiểm soát quyền theo bản ghi dữ liệu

Với các bổ sung trên, backend sẽ đủ nền tảng để hỗ trợ an toàn cho toàn bộ module nghiệp vụ phía sau.
