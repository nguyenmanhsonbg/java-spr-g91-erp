# Đặc tả chi tiết module User Management

## 1. Thông tin tài liệu

- **Tài liệu:** Detailed Specification – User Management Module
- **Project:** G90 Steel Business Management System
- **Module:** User Management
- **Phạm vi:** Đặc tả chi tiết nghiệp vụ, phân quyền, dữ liệu, validation, trạng thái, API đề xuất, mapping database và các rule xử lý cho module User Management
- **Nguồn tham chiếu:** Project Spec (`Spec_Project_G90.md`), Database Schema (`V1__init.sql`)

---

## 2. Mục tiêu nghiệp vụ

Module **User Management** cung cấp các chức năng quản lý vòng đời đăng nhập và hồ sơ người dùng cho toàn hệ thống G90 Steel.

Mục tiêu chính:

- Cho phép Guest đăng ký tài khoản Customer
- Cho phép người dùng đăng nhập / đăng xuất
- Cho phép người dùng đổi mật khẩu
- Cho phép người dùng yêu cầu reset mật khẩu
- Cho phép người dùng xem hồ sơ cá nhân
- Cho phép người dùng cập nhật hồ sơ cá nhân

Đây là module nền tảng phục vụ xác thực, phân quyền và quản lý thông tin tài khoản người dùng trong toàn hệ thống.

---

## 3. Phạm vi module

### 3.1 Trong phạm vi
Theo spec dự án, module này bao gồm các use case:

1. Register
2. Login
3. Logout
4. Change Password
5. Reset Password
6. View Profile
7. Update Profile

### 3.2 Ngoài phạm vi
Không bao gồm:
- Tạo tài khoản nội bộ bởi Owner
- Cập nhật role hoặc status của user khác
- Deactivate user account
- Quản trị phân quyền hệ thống
- Dashboard / reporting
- IAM / SSO / OAuth2 integration nâng cao

Các chức năng trên thuộc module khác, chủ yếu là **Account Management**.

---

## 4. Actor và phân quyền

## 4.1 Actor liên quan

### Guest
- Có thể đăng ký tài khoản Customer

### Owner
- Có thể login, logout, change password, reset password, view profile, update profile

### Accountant
- Có thể login, logout, change password, reset password, view profile, update profile

### Warehouse
- Có thể login, logout, change password, reset password, view profile, update profile

### Customer
- Có thể login, logout, change password, reset password, view profile, update profile

---

## 5. Danh sách use case trong module

| ID | Use case | Actor |
|---|---|---|
| UM-01 | Register | Guest |
| UM-02 | Login | Owner, Accountant, Warehouse, Customer |
| UM-03 | Logout | Owner, Accountant, Warehouse, Customer |
| UM-04 | Change Password | Owner, Accountant, Warehouse, Customer |
| UM-05 | Reset Password | Owner, Accountant, Warehouse, Customer |
| UM-06 | View Profile | Owner, Accountant, Warehouse, Customer |
| UM-07 | Update Profile | Owner, Accountant, Warehouse, Customer |

---

## 6. Database liên quan

Theo schema hiện tại, module User Management chủ yếu sử dụng các bảng:

### `roles`
Lưu danh mục vai trò người dùng.

Các cột:
- `id`
- `name`
- `description`

### `users`
Lưu thông tin tài khoản người dùng.

Các cột:
- `id`
- `role_id`
- `full_name`
- `email`
- `password_hash`
- `phone`
- `address`
- `status`
- `created_at`
- `updated_at`

### `customers`
Lưu thông tin business profile cho user có role Customer.

Các cột liên quan:
- `id`
- `user_id`
- `company_name`
- `tax_code`
- `address`
- `contact_person`
- `phone`
- `email`
- `customer_type`
- `credit_limit`
- `status`
- `created_at`

### `audit_logs`
Lưu log thao tác quan trọng.

Các cột:
- `id`
- `user_id`
- `action`
- `entity_type`
- `entity_id`
- `old_value`
- `new_value`
- `created_at`

---

## 7. Thực thể nghiệp vụ và mapping

## 7.1 User account
Map chủ yếu vào bảng `users`.

### Mapping field cơ bản

| Business Field | DB Field |
|---|---|
| User ID | users.id |
| Role | users.role_id |
| Full name | users.full_name |
| Email | users.email |
| Password hash | users.password_hash |
| Phone | users.phone |
| Address | users.address |
| Status | users.status |
| Created at | users.created_at |
| Updated at | users.updated_at |

## 7.2 Customer profile
Khi register thành công, hệ thống ngoài `users` còn tạo `customers`.

| Business Field | DB Field |
|---|---|
| Customer profile ID | customers.id |
| Linked user | customers.user_id |
| Company name | customers.company_name |
| Tax code | customers.tax_code |
| Contact person | customers.contact_person |
| Phone | customers.phone |
| Email | customers.email |
| Customer type | customers.customer_type |
| Status | customers.status |

---

## 8. Trạng thái và quy ước dữ liệu

## 8.1 Account status
Theo spec và schema, các trạng thái có thể dùng:

- `ACTIVE`
- `INACTIVE`
- `LOCKED` (được implied trong spec/message table)

### Ý nghĩa
- `ACTIVE`: dùng được bình thường
- `INACTIVE`: không được phép login
- `LOCKED`: bị khóa, không được phép login

## 8.2 Role
Theo spec, các role logic là:

- `OWNER`
- `ACCOUNTANT`
- `WAREHOUSE`
- `CUSTOMER`

### Lưu ý
Schema `roles.name` chưa ràng buộc enum, nên cần quy ước thống nhất tên role ở tầng nghiệp vụ.

---

## 9. Business rules áp dụng cho module

Các business rules từ spec áp dụng trực tiếp cho module này:

1. Chỉ authenticated users mới truy cập chức năng role-specific
2. Guest chỉ dùng được public functions như registration
3. Password phải đáp ứng yêu cầu tối thiểu và được mã hóa
4. Deactivated account không được đăng nhập
5. Email phải đúng định dạng cơ bản
6. Email phải unique
7. Các hành động quan trọng phải được log
8. Role và email không được sửa ở use case Update Profile
9. Register tạo account với role = Customer
10. Dữ liệu lịch sử không được mất khi account không còn active

---

# 10. Đặc tả chi tiết từng use case

---

# 10.1 UM-01 Register

## 10.1.1 Mục tiêu
Cho phép Guest đăng ký tài khoản mới để trở thành Customer của hệ thống.

## 10.1.2 Actor
- Primary actor: Guest
- Secondary actor: Email Provider (theo spec)
- System

## 10.1.3 Tiền điều kiện
- User chưa đăng nhập
- Email chưa tồn tại trong hệ thống
- Form đăng ký được truy cập từ public area

## 10.1.4 Hậu điều kiện thành công
- Tạo một bản ghi mới trong `users`
- Gán role = `CUSTOMER`
- Tạo một bản ghi mới trong `customers`
- Trạng thái account mặc định là `ACTIVE` hoặc trạng thái onboarding do hệ thống định nghĩa
- Có thể redirect sang login page

## 10.1.5 Dữ liệu đầu vào
Theo spec gốc:

- Full name
- Email address
- Password
- Confirm password

## 10.1.6 Validation
- Tất cả trường bắt buộc phải nhập
- Email phải chứa `@`
- Email phải unique
- Password tối thiểu 6 ký tự
- Password và Confirm password phải khớp

## 10.1.7 Main flow
1. Guest mở màn hình Register
2. Nhập full name, email, password, confirm password
3. Nhấn đăng ký
4. Hệ thống validate dữ liệu
5. Hệ thống tìm role `CUSTOMER`
6. Hệ thống hash password
7. Tạo `users`
8. Tạo `customers` liên kết theo `user_id`
9. Ghi audit log nếu policy yêu cầu
10. Trả thông báo thành công
11. Chuyển hướng sang login

## 10.1.8 Alternative flow
### A1. Thiếu dữ liệu bắt buộc
- Hệ thống highlight field lỗi
- Không tạo dữ liệu

### A2. Email sai định dạng
- Báo lỗi
- Không tạo dữ liệu

### A3. Password không khớp
- Báo lỗi
- Không tạo dữ liệu

### A4. Email đã tồn tại
- Báo lỗi duplicate
- Không tạo dữ liệu

## 10.1.9 Exception flow
### E1. Không tìm thấy role CUSTOMER
- System error ở tầng cấu hình dữ liệu

### E2. Insert users thành công nhưng insert customers lỗi
- Rollback toàn bộ transaction

## 10.1.10 Mapping DB đề xuất
### Insert `users`
- `id`
- `role_id`
- `full_name`
- `email`
- `password_hash`
- `status`
- `created_at`
- `updated_at`

### Insert `customers`
- `id`
- `user_id`
- `contact_person` = full name
- `email`
- `phone` = null
- `address` = null
- `status` = ACTIVE

## 10.1.11 Request model đề xuất
```json
{
  "fullName": "Nguyen Van A",
  "email": "customer@g90steel.vn",
  "password": "123456",
  "confirmPassword": "123456"
}
```

## 10.1.12 Response model đề xuất
```json
{
  "message": "Register successfully",
  "userId": "uuid-user",
  "redirectTo": "/login"
}
```

---

# 10.2 UM-02 Login

## 10.2.1 Mục tiêu
Xác thực người dùng và cấp quyền truy cập theo role.

## 10.2.2 Actor
- Owner
- Accountant
- Warehouse
- Customer

## 10.2.3 Tiền điều kiện
- User có account hợp lệ
- Account không bị inactive / locked

## 10.2.4 Hậu điều kiện thành công
- User được xác thực
- Phiên đăng nhập được tạo
- Hoặc token được cấp nếu hệ thống dùng JWT
- Redirect đến landing page phù hợp vai trò

## 10.2.5 Input
- Email
- Password

## 10.2.6 Validation
- Email bắt buộc
- Password bắt buộc
- Email đúng format cơ bản
- Account tồn tại
- Password đúng
- Account status cho phép login

## 10.2.7 Main flow
1. User mở màn hình login
2. Nhập email và password
3. Nhấn đăng nhập
4. Hệ thống validate input
5. Tìm `users` theo email
6. So khớp password hash
7. Kiểm tra status
8. Tạo session/token
9. Trả thông tin user cơ bản và role
10. Redirect theo role

## 10.2.8 Alternative flow
### A1. Thiếu email/password
- Báo lỗi input

### A2. Email sai format
- Báo lỗi format

### A3. Sai tài khoản hoặc mật khẩu
- Trả lỗi generic, không lộ thông tin hệ thống

## 10.2.9 Exception flow
### E1. Account inactive / locked
- Từ chối login

### E2. System unavailable
- Trả lỗi hệ thống

## 10.2.10 Request model đề xuất
```json
{
  "email": "owner@g90steel.vn",
  "password": "123456"
}
```

## 10.2.11 Response model đề xuất
```json
{
  "accessToken": "jwt-or-session-token",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid-user",
    "fullName": "Owner G90",
    "email": "owner@g90steel.vn",
    "role": "OWNER",
    "status": "ACTIVE"
  }
}
```

### Nếu dùng session-based auth
Có thể trả:
```json
{
  "message": "Login successfully",
  "user": {
    "id": "uuid-user",
    "fullName": "Owner G90",
    "email": "owner@g90steel.vn",
    "role": "OWNER"
  }
}
```

---

# 10.3 UM-03 Logout

## 10.3.1 Mục tiêu
Kết thúc phiên đăng nhập an toàn.

## 10.3.2 Actor
- Owner
- Accountant
- Warehouse
- Customer

## 10.3.3 Tiền điều kiện
- User đang đăng nhập

## 10.3.4 Hậu điều kiện
- Session/token hiện tại không còn hiệu lực
- User bị đăng xuất khỏi hệ thống

## 10.3.5 Main flow
1. User nhấn Logout
2. Hệ thống xác định session/token hiện tại
3. Hệ thống invalidate session/token
4. Trả thông báo thành công
5. Redirect về homepage hoặc login page

## 10.3.6 Alternative flow
### A1. Session đã hết hạn
- Hệ thống vẫn cho ra trạng thái logout thành công logic hoặc báo session expired

## 10.3.7 Response model đề xuất
```json
{
  "message": "Logout successfully"
}
```

---

# 10.4 UM-04 Change Password

## 10.4.1 Mục tiêu
Cho phép user đã đăng nhập đổi mật khẩu.

## 10.4.2 Actor
- Owner
- Accountant
- Warehouse
- Customer

## 10.4.3 Tiền điều kiện
- User đang đăng nhập
- User biết current password

## 10.4.4 Hậu điều kiện
- `users.password_hash` được cập nhật mới
- Password cũ không còn dùng được

## 10.4.5 Input
- Current password
- New password
- Confirm new password

## 10.4.6 Validation
- Tất cả trường bắt buộc
- Current password phải đúng
- New password tối thiểu 6 ký tự
- New password và confirm phải khớp
- Nên có rule không cho trùng mật khẩu cũ

## 10.4.7 Main flow
1. User mở màn hình Change Password
2. Nhập current password, new password, confirm
3. Nhấn xác nhận
4. Hệ thống validate input
5. So khớp current password
6. Hash password mới
7. Update `users.password_hash`
8. Ghi audit log
9. Trả thành công

## 10.4.8 Alternative flow
### A1. Thiếu dữ liệu
- Báo lỗi

### A2. Current password sai
- Báo lỗi

### A3. Confirm password không khớp
- Báo lỗi

## 10.4.9 Request model đề xuất
```json
{
  "currentPassword": "123456",
  "newPassword": "654321",
  "confirmNewPassword": "654321"
}
```

## 10.4.10 Response model đề xuất
```json
{
  "message": "Password changed successfully"
}
```

---

# 10.5 UM-05 Reset Password

## 10.5.1 Mục tiêu
Cho phép user yêu cầu reset password khi quên mật khẩu.

## 10.5.2 Actor
- Owner
- Accountant
- Warehouse
- Customer

## 10.5.3 Tiền điều kiện
- User nhớ email đã đăng ký

## 10.5.4 Hậu điều kiện
- Hệ thống tạo reset token
- Gửi link reset qua email
- User có thể đặt lại mật khẩu

## 10.5.5 Input
- Registered email address

## 10.5.6 Validation
- Email bắt buộc
- Email đúng format
- Email tồn tại trong hệ thống

## 10.5.7 Main flow
1. User mở màn hình Forgot Password
2. Nhập email
3. Hệ thống validate email
4. Tìm account theo email
5. Sinh reset token
6. Lưu token vào nơi lưu trữ phù hợp
7. Gửi email reset link
8. Trả thông báo đã gửi email

## 10.5.8 Alternative flow
### A1. Email sai format
- Báo lỗi

### A2. Email không tồn tại
- Có thể trả thông báo generic để tránh lộ thông tin tài khoản

## 10.5.9 Exception flow
### E1. Email service failure
- Trả lỗi gửi mail thất bại

## 10.5.10 Khoảng trống schema hiện tại
Database hiện tại **chưa có bảng lưu reset token**, ví dụ:
- `password_reset_tokens`
hoặc trường tương đương trong `users`

Do đó cần bổ sung schema nếu implement đầy đủ luồng reset password.

## 10.5.11 Request model đề xuất
```json
{
  "email": "customer@g90steel.vn"
}
```

## 10.5.12 Response model đề xuất
```json
{
  "message": "A confirmation email has been sent"
}
```

---

# 10.6 UM-06 View Profile

## 10.6.1 Mục tiêu
Cho phép user xem thông tin hồ sơ cá nhân của chính mình.

## 10.6.2 Actor
- Owner
- Accountant
- Warehouse
- Customer

## 10.6.3 Tiền điều kiện
- User đã đăng nhập

## 10.6.4 Hậu điều kiện
- Không thay đổi dữ liệu
- Trả thông tin hồ sơ hiện tại

## 10.6.5 Dữ liệu hiển thị
Theo spec:
- Name
- Email
- Role
- Contact information

## 10.6.6 Main flow
1. User mở trang profile
2. Hệ thống lấy user từ security context
3. Query dữ liệu `users`
4. Join `roles`
5. Trả dữ liệu profile

### Nếu là Customer
Có thể mở rộng join `customers` để hiển thị thêm business info nếu UI yêu cầu, nhưng use case gốc chủ yếu nói profile account.

## 10.6.7 Response model đề xuất
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
  "updatedAt": "2026-03-14T09:00:00+07:00"
}
```

---

# 10.7 UM-07 Update Profile

## 10.7.1 Mục tiêu
Cho phép user cập nhật hồ sơ cá nhân của chính mình.

## 10.7.2 Actor
- Owner
- Accountant
- Warehouse
- Customer

## 10.7.3 Tiền điều kiện
- User đã đăng nhập
- Profile tồn tại

## 10.7.4 Hậu điều kiện
- Thông tin profile được cập nhật
- `updated_at` thay đổi
- Có audit log

## 10.7.5 Dữ liệu có thể cập nhật
Theo spec:
- Full name
- Contact information
- Phone number
- Address

## 10.7.6 Dữ liệu không được cập nhật ở use case này
- Email
- Role

## 10.7.7 Validation
- Full name bắt buộc nếu UI yêu cầu
- Không vượt quá max length
- Email không cho sửa
- Role không cho sửa
- Có thể chặn update nếu không có thay đổi

## 10.7.8 Main flow
1. User mở form Update Profile
2. Hệ thống load dữ liệu hiện tại
3. User sửa full name / phone / address
4. Nhấn lưu
5. Hệ thống validate
6. Update `users`
7. Ghi audit log
8. Trả profile mới

## 10.7.9 Alternative flow
### A1. Thiếu dữ liệu bắt buộc
- Báo lỗi

### A2. Quá độ dài cho phép
- Báo lỗi

### A3. Không có thay đổi
- Trả thông báo no changes detected hoặc success logic tùy policy

## 10.7.10 Request model đề xuất
```json
{
  "fullName": "Nguyen Van B",
  "phone": "0908888888",
  "address": "Ho Chi Minh"
}
```

## 10.7.11 Response model đề xuất
```json
{
  "message": "Profile updated successfully",
  "profile": {
    "id": "uuid-user",
    "fullName": "Nguyen Van B",
    "email": "customer@g90steel.vn",
    "role": "CUSTOMER",
    "phone": "0908888888",
    "address": "Ho Chi Minh",
    "status": "ACTIVE"
  }
}
```

---

## 11. API đề xuất cho module

| API ID | API Name | Method | Endpoint |
|---|---|---|---|
| UM-API-01 | Register | POST | /api/auth/register |
| UM-API-02 | Login | POST | /api/auth/login |
| UM-API-03 | Logout | POST | /api/auth/logout |
| UM-API-04 | Change Password | POST / PATCH | /api/auth/change-password |
| UM-API-05 | Request Reset Password | POST | /api/auth/forgot-password |
| UM-API-06 | Confirm Reset Password | POST | /api/auth/reset-password |
| UM-API-07 | View My Profile | GET | /api/users/me |
| UM-API-08 | Update My Profile | PUT / PATCH | /api/users/me |

### Lưu ý
Spec gốc chỉ nêu **Reset Password** ở mức business use case, nhưng để implement đầy đủ qua email token, thực tế nên tách thành:
- Request reset password
- Confirm reset password

---

## 12. Validation tổng hợp theo API

## 12.1 Register
- `fullName` required
- `email` required, format đúng, unique
- `password` required, min 6
- `confirmPassword` required, match password

## 12.2 Login
- `email` required
- `password` required
- credentials đúng
- status hợp lệ

## 12.3 Change Password
- `currentPassword` required
- `newPassword` required
- `confirmNewPassword` required
- confirm phải match
- current password đúng

## 12.4 Forgot Password
- `email` required
- format đúng

## 12.5 Update Profile
- chỉ cho sửa field được phép
- không cho sửa `email`
- không cho sửa `role`

---

## 13. Yêu cầu transaction

Các luồng sau phải chạy transaction:

### Register
1. Insert users
2. Insert customers
3. Insert audit log (nếu có)
4. Commit

Nếu lỗi bất kỳ bước nào:
- rollback toàn bộ

### Change Password
1. Validate current password
2. Update password_hash
3. Insert audit log
4. Commit

### Update Profile
1. Update users
2. Insert audit log
3. Commit

---

## 14. Yêu cầu audit log

Các action nên được log:

- `REGISTER_USER`
- `LOGIN_SUCCESS` hoặc log auth event riêng nếu policy cho phép
- `CHANGE_PASSWORD`
- `REQUEST_RESET_PASSWORD`
- `RESET_PASSWORD_SUCCESS`
- `UPDATE_PROFILE`

### Ví dụ log update profile
```json
{
  "action": "UPDATE_PROFILE",
  "entityType": "USER",
  "entityId": "uuid-user",
  "oldValue": {
    "fullName": "Nguyen Van A",
    "phone": "0901111111"
  },
  "newValue": {
    "fullName": "Nguyen Van B",
    "phone": "0902222222"
  }
}
```

---

## 15. Error handling đề xuất

### Nhóm lỗi xác thực / dữ liệu
- Missing required field
- Invalid email format
- Password mismatch
- Incorrect username or password
- Account inactive or locked
- Exceed max length

### Nhóm lỗi hệ thống
- Email service unavailable
- System under maintenance
- Unexpected error

---

## 16. Khoảng trống giữa spec và database hiện tại

Có một số điểm cần lưu ý khi bám spec để implement:

### 16.1 Chưa có bảng lưu reset password token
Schema chưa có:
- `password_reset_tokens`
- hoặc cột token/reset_expired_at trong `users`

=> chưa đủ để implement hoàn chỉnh reset password qua email

### 16.2 Chưa có email verification flow
Spec nhắc Email Provider, nhưng DB chưa có:
- email verification token
- verified flag

### 16.3 Chưa có failed login / lockout policy
Spec có trạng thái locked implied, nhưng schema chưa có:
- failed_login_count
- locked_until
- last_login_at

### 16.4 Register chỉ có dữ liệu account cơ bản
Schema `customers` thiên về business customer, nhưng form register trong spec chỉ gồm:
- full name
- email
- password

=> cần định nghĩa rõ sau register:
- có tạo customer profile tối giản ngay
- hay tạo sau ở bước onboarding

---

## 17. Đề xuất cải tiến schema nếu muốn hoàn thiện module hơn

### 17.1 Bảng reset password token
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

### 17.2 Bổ sung thông tin đăng nhập
```sql
ALTER TABLE users
ADD COLUMN last_login_at TIMESTAMP NULL,
ADD COLUMN failed_login_count INT DEFAULT 0,
ADD COLUMN locked_until TIMESTAMP NULL;
```

### 17.3 Email verification
```sql
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
```

---

## 18. Acceptance criteria tổng hợp

Module User Management được xem là đáp ứng yêu cầu khi:

1. Guest đăng ký được Customer account mới
2. Hệ thống ngăn duplicate email
3. User login được với account hợp lệ
4. Account inactive / locked không login được
5. User logout được an toàn
6. User đổi password được khi current password đúng
7. User request reset password được qua email
8. User xem được profile của chính mình
9. User cập nhật được profile của chính mình
10. Email và role không bị sửa trong Update Profile
11. Mọi thay đổi quan trọng có audit log
12. Dữ liệu password luôn lưu dưới dạng hash

---

## 19. Test scenarios cốt lõi

### TC01 – Register thành công
- Guest nhập đủ thông tin hợp lệ
- Email chưa tồn tại
- Kỳ vọng: tạo `users` + `customers`

### TC02 – Register email trùng
- Email đã tồn tại trong `users`
- Kỳ vọng: báo lỗi duplicate

### TC03 – Register password mismatch
- password != confirmPassword
- Kỳ vọng: báo lỗi

### TC04 – Login thành công
- Account active, password đúng
- Kỳ vọng: login thành công

### TC05 – Login sai mật khẩu
- Password sai
- Kỳ vọng: báo lỗi credentials

### TC06 – Login account inactive
- status = INACTIVE
- Kỳ vọng: từ chối login

### TC07 – Change password thành công
- current password đúng
- new password hợp lệ
- Kỳ vọng: update password_hash

### TC08 – Change password sai current password
- Kỳ vọng: báo lỗi

### TC09 – View profile thành công
- User đã login
- Kỳ vọng: trả đúng profile chính mình

### TC10 – Update profile thành công
- sửa fullName/phone/address
- Kỳ vọng: update thành công

### TC11 – Update profile cố sửa email
- Kỳ vọng: bị chặn hoặc ignored theo policy

### TC12 – Forgot password với email hợp lệ
- Kỳ vọng: sinh token và gửi mail

---

## 20. Kết luận

Module **User Management** là module lõi cho xác thực và hồ sơ người dùng của G90 Steel.

Với schema hiện tại, hệ thống đã đủ nền tảng để triển khai tốt các chức năng:
- register
- login
- logout
- change password
- view profile
- update profile

Tuy nhiên, để hoàn chỉnh theo spec cho use case **Reset Password**, cần bổ sung:
- bảng lưu reset token
- cơ chế gửi email và xác thực token
- có thể thêm thông tin lockout / failed login nếu muốn tăng độ chặt chẽ bảo mật

Nếu triển khai thực tế, nên tách rõ:
- **User Management**: self-service account functions
- **Account Management**: owner quản lý user nội bộ
