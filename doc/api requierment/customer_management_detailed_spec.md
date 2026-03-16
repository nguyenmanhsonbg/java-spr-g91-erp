# Đặc tả chi tiết module Customer Management

## 1. Thông tin tài liệu

- **Tài liệu:** Detailed Specification – Customer Management Module
- **Project:** G90 Steel Business Management System
- **Module:** Customer Management
- **Phạm vi:** Đặc tả chi tiết nghiệp vụ, phân quyền, dữ liệu, validation, trạng thái, API đề xuất, mapping database, business rules, transaction handling, audit log và khoảng trống giữa spec với schema hiện tại cho module Customer Management
- **Nguồn tham chiếu:** Project Spec (`Spec_Project_G90.md`), Database Schema (`V1__init.sql`)

---

## 2. Mục tiêu nghiệp vụ

Module **Customer Management** cho phép **Accountant** quản lý hồ sơ khách hàng doanh nghiệp trong hệ thống G90 Steel.

Mục tiêu chính:

- tạo hồ sơ khách hàng mới
- xem thông tin khách hàng
- cập nhật thông tin khách hàng
- vô hiệu hóa khách hàng khi cần nhưng vẫn giữ lịch sử
- phục vụ các luồng liên quan như quotation, contract, project, invoice, debt

Đây là module lõi cho toàn bộ quy trình thương mại vì mọi quotation, contract, project, invoice và debt đều gắn với customer.

---

## 3. Phạm vi module

### 3.1 Trong phạm vi
Theo spec, module này bao gồm các use case:

1. Create Customer
2. View Customer Information
3. Update Customer Information
4. Disable Customer

### 3.2 Ngoài phạm vi
Không bao gồm:
- self-register của Guest/Customer
- quản lý tài khoản nội bộ
- quản lý quotation
- tạo contract
- project management chi tiết
- payment / debt handling chi tiết
- hợp nhất (merge) khách hàng
- import CRM nâng cao
- marketing communication management

---

## 4. Actor và phân quyền

## 4.1 Actor chính
- **Accountant**

## 4.2 Actor liên quan
- **Owner**: có thể xem các dữ liệu liên quan ở module khác, nhưng không phải actor chính của Customer Management theo spec chi tiết
- **Customer**: là đối tượng dữ liệu được quản lý
- **System**: validate dữ liệu, sinh mã customer, lưu DB, ghi audit log, kiểm tra ràng buộc với các module khác

## 4.3 Quyền truy cập
- Chỉ **Accountant** được tạo / cập nhật / disable customer theo spec
- Customer không tự dùng module này để sửa hồ sơ business customer tổng thể
- Guest không truy cập module này
- Accountant có thể xem danh sách và chi tiết khách hàng
- Các thao tác nhạy cảm như thay đổi credit limit, payment terms, disable customer phải chịu ràng buộc nghiệp vụ

---

## 5. Danh sách use case trong module

| ID | Use case | Primary Actor |
|---|---|---|
| CM-01 | Create Customer | Accountant |
| CM-02 | View Customer Information | Accountant |
| CM-03 | Update Customer Information | Accountant |
| CM-04 | Disable Customer | Accountant |

---

## 6. Database liên quan

Theo schema hiện tại, module Customer Management chủ yếu sử dụng các bảng:

### `customers`
Bảng trung tâm của module.

Các cột:
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

### `users`
Dùng khi customer có account liên kết hoặc khi cần tạo portal login.

Các cột liên quan:
- `id`
- `role_id`
- `full_name`
- `email`
- `phone`
- `address`
- `status`
- `created_at`
- `updated_at`

### `roles`
Dùng để xác định role `CUSTOMER` khi cần tạo account customer.

### `projects`
Dùng để kiểm tra ràng buộc khi disable customer hoặc view related projects.

Các cột liên quan:
- `id`
- `customer_id`
- `status`

### `quotations`
Dùng để xem quotation liên quan hoặc kiểm tra ràng buộc disable.

### `contracts`
Dùng để xem contract liên quan hoặc kiểm tra trạng thái giao dịch.

### `invoices`
Dùng để xem invoice và tính trạng thái tài chính.

### `payments`
### `payment_allocations`
Dùng để phân tích lịch sử thanh toán và dư nợ.

### `audit_logs`
Lưu log các thao tác quan trọng.

---

## 7. Thực thể nghiệp vụ và mapping

## 7.1 Customer business profile
Map vào bảng `customers`.

| Business Field | DB Field |
|---|---|
| Customer ID | customers.id |
| Linked user account | customers.user_id |
| Company name | customers.company_name |
| Tax code | customers.tax_code |
| Address | customers.address |
| Contact person | customers.contact_person |
| Phone | customers.phone |
| Email | customers.email |
| Customer type / classification | customers.customer_type |
| Credit limit | customers.credit_limit |
| Status | customers.status |
| Created at | customers.created_at |

## 7.2 Customer portal account
Nếu hệ thống tạo customer login account cùng lúc, sẽ map thêm vào `users`.

| Business Field | DB Field |
|---|---|
| User account ID | users.id |
| Role | users.role_id |
| Full name | users.full_name |
| Email | users.email |
| Phone | users.phone |
| Address | users.address |
| Status | users.status |

---

## 8. Trạng thái và quy ước dữ liệu

## 8.1 Customer status
Theo spec và schema, có thể dùng:

- `ACTIVE`
- `INACTIVE`

### Ý nghĩa
- `ACTIVE`: customer đang hoạt động, có thể tạo giao dịch mới
- `INACTIVE`: customer bị disable, không được tạo giao dịch mới nhưng dữ liệu lịch sử vẫn giữ nguyên

## 8.2 Customer type / classification
Theo spec:
- `Retail`
- `Contractor`
- `Distributor`

### Lưu ý
Schema `customers.customer_type` đang là `VARCHAR(50)`, nên cần chuẩn hóa enum ở tầng nghiệp vụ.

## 8.3 Customer identifier
Spec nêu các định danh quan trọng:
- tax code
- phone number hoặc tax code dùng như unique business identifier theo business rule
- customer code format `CUST-XXX sequential` theo spec, nhưng schema hiện tại **chưa có cột customer_code**

---

## 9. Business rules áp dụng cho module

Các business rules và nội dung đặc tả quan trọng:

1. Chỉ Accountant được quản lý customer
2. Mỗi customer phải có định danh duy nhất, ít nhất theo tax code hoặc phone/tax code policy
3. Tax code phải 10–13 chữ số
4. Credit limit phụ thuộc loại khách hàng
5. Credit limit changes cần approval ở mức nghiệp vụ
6. Price group changes chỉ áp dụng cho giao dịch tương lai
7. Không được disable customer nếu còn debt lớn hơn ngưỡng rule
8. Không được disable customer nếu còn active projects hoặc open orders
9. Disable customer là soft-disable, không xóa vật lý dữ liệu
10. Mọi thao tác create/update/disable phải có audit log
11. Dữ liệu khách hàng là dữ liệu lõi cho quotation, contract, project, invoice, debt

---

# 10. Đặc tả chi tiết từng use case

---

# 10.1 CM-01 Create Customer

## 10.1.1 Mục tiêu
Cho phép Accountant tạo hồ sơ business customer mới.

## 10.1.2 Actor
- Primary actor: Accountant
- Supporting actor: System

## 10.1.3 Tiền điều kiện
- Accountant đã đăng nhập
- Role = `ACCOUNTANT`
- Account status = `ACTIVE`

## 10.1.4 Hậu điều kiện thành công
- Tạo mới một bản ghi trong `customers`
- Có thể tạo thêm portal login account cho customer nếu policy hệ thống yêu cầu
- Ghi audit log
- Customer sẵn sàng để dùng cho quotation/contract/project

## 10.1.5 Dữ liệu đầu vào
Theo spec:

- company name
- tax code
- address
- primary contact person
- phone number
- email
- customer type / classification
- price group
- credit limit
- payment terms

## 10.1.6 Khoảng trống so với schema hiện tại
Schema `customers` hiện có:
- company_name
- tax_code
- address
- contact_person
- phone
- email
- customer_type
- credit_limit
- status

Nhưng **chưa có**:
- `customer_code`
- `price_group`
- `payment_terms`
- `assigned_sales_representative`
- nhiều contact persons

## 10.1.7 Validation
- company name bắt buộc
- tax code bắt buộc
- tax code phải 10–13 ký tự số
- tax code unique
- email đúng format nếu có nhập
- phone đúng format nếu policy yêu cầu
- customer type phải thuộc tập giá trị hợp lệ
- credit limit >= 0
- email portal login nếu tạo account thì phải unique trong `users.email`

## 10.1.8 Main flow
1. Accountant mở màn hình Create Customer
2. Nhập thông tin customer
3. Nhấn lưu
4. Hệ thống validate dữ liệu
5. Kiểm tra duplicate tax code
6. Xác định customer type và default credit policy
7. Sinh mã customer theo nghiệp vụ
8. Tạo bản ghi `customers`
9. Nếu có yêu cầu tạo portal account, tạo thêm `users`
10. Ghi audit log
11. Trả kết quả thành công

## 10.1.9 Alternative flow
### A1. Quick create
- Chỉ nhập bộ trường tối thiểu
- Bổ sung sau ở update flow

### A2. Import from CRM
- Được nêu trong spec như hướng mở rộng
- Ngoài phạm vi schema hiện tại

### A3. Tạo customer kèm portal login
- Nếu policy cho phép, tạo `users` role CUSTOMER cùng lúc

## 10.1.10 Exception flow
### E1. Duplicate tax code
- Báo lỗi, không lưu dữ liệu

### E2. Invalid business registration / tax info
- Báo lỗi nghiệp vụ

### E3. Insert customer thành công nhưng insert user thất bại
- rollback nếu chọn chế độ tạo kèm account trong cùng transaction

## 10.1.11 Request model đề xuất
```json
{
  "companyName": "ABC Steel Construction Co., Ltd",
  "taxCode": "0101234567",
  "address": "Ha Noi",
  "contactPerson": "Nguyen Van A",
  "phone": "0901234567",
  "email": "contact@abcsteel.vn",
  "customerType": "CONTRACTOR",
  "priceGroup": "CONTRACTOR",
  "creditLimit": 500000000,
  "paymentTerms": "70% on delivery, 30% within 30 days",
  "createPortalAccount": false
}
```

## 10.1.12 Response model đề xuất
```json
{
  "id": "customer-uuid",
  "customerCode": "CUST-001",
  "message": "Customer created successfully"
}
```

---

# 10.2 CM-02 View Customer Information

## 10.2.1 Mục tiêu
Cho phép Accountant xem hồ sơ chi tiết của customer.

## 10.2.2 Actor
- Primary actor: Accountant

## 10.2.3 Tiền điều kiện
- Accountant đã đăng nhập
- Customer tồn tại

## 10.2.4 Hậu điều kiện
- Không thay đổi dữ liệu
- Trả thông tin customer và các dữ liệu liên quan

## 10.2.5 Dữ liệu hiển thị theo spec
- Basic info
- Contact persons
- Order history
- Payment history
- Documents
- Classification
- Assigned price group
- Current credit limit
- Total outstanding debt
- Associated projects
- Summary of recent transactions

## 10.2.6 Khoảng trống schema hiện tại
Schema hiện chưa có đủ bảng cho:
- documents
- multiple contact persons
- assigned price group riêng
- debt summary snapshot
- order history tách biệt nếu không dùng contracts/quotations/invoices để suy luận

## 10.2.7 Main flow
1. Accountant mở customer detail
2. Hệ thống lấy `customers`
3. Hệ thống tổng hợp dữ liệu liên quan từ:
   - quotations
   - contracts
   - invoices
   - payments
   - projects
4. Hệ thống trả về customer profile detail

## 10.2.8 Response model đề xuất
```json
{
  "id": "customer-uuid",
  "customerCode": "CUST-001",
  "companyName": "ABC Steel Construction Co., Ltd",
  "taxCode": "0101234567",
  "address": "Ha Noi",
  "contactPerson": "Nguyen Van A",
  "phone": "0901234567",
  "email": "contact@abcsteel.vn",
  "customerType": "CONTRACTOR",
  "priceGroup": "CONTRACTOR",
  "creditLimit": 500000000,
  "status": "ACTIVE",
  "outstandingDebt": 120000000,
  "projectCount": 3,
  "quotationCount": 5,
  "contractCount": 4,
  "invoiceCount": 6,
  "recentTransactions": []
}
```

---

# 10.3 CM-03 Update Customer Information

## 10.3.1 Mục tiêu
Cho phép Accountant cập nhật hồ sơ customer hiện có.

## 10.3.2 Actor
- Primary actor: Accountant

## 10.3.3 Tiền điều kiện
- Accountant đã đăng nhập
- Customer tồn tại

## 10.3.4 Hậu điều kiện
- Dữ liệu customer được cập nhật
- Có audit log
- Các thay đổi ảnh hưởng giao dịch tương lai phải được ghi nhận đúng policy

## 10.3.5 Dữ liệu có thể cập nhật theo spec
- assigned sales representative
- customer classification
- credit limit
- payment terms
- contact information
- addresses

## 10.3.6 Dữ liệu hạn chế
- tax code có thể là restricted field
- company name có thể là restricted field
- thay đổi các trường trọng yếu có thể cần validation bổ sung hoặc approval

## 10.3.7 Khoảng trống schema hiện tại
Schema hiện tại chưa có cột:
- `assigned_sales_representative`
- `payment_terms`
- `price_group`

## 10.3.8 Validation
- customer phải tồn tại
- tax code mới nếu cho phép sửa phải unique
- credit limit >= 0
- customer type hợp lệ
- email đúng format nếu có
- không cho cập nhật vượt quá max length
- thay đổi restricted fields phải qua policy check

## 10.3.9 Main flow
1. Accountant mở form Update Customer
2. Hệ thống load dữ liệu hiện tại
3. Accountant sửa các trường cho phép
4. Nhấn lưu
5. Hệ thống validate dữ liệu
6. Hệ thống kiểm tra impact business rules
7. Update `customers`
8. Nếu customer có portal account và email/contact được đồng bộ, có thể update `users`
9. Ghi audit log
10. Trả kết quả thành công

## 10.3.10 Alternative flow
### A1. Bulk update
- Được spec nhắc đến như hướng mở rộng
- ngoài phạm vi schema hiện tại

### A2. Request verification
- Nếu thay đổi field nhạy cảm như tax code
- có thể cần review nội bộ

## 10.3.11 Exception flow
### E1. Modify restricted field
- Chặn cập nhật hoặc yêu cầu quy trình đặc biệt

### E2. Active orders prevent certain changes
- Nếu customer đang có giao dịch đang hoạt động, có thể không cho đổi một số thông tin nền tảng

### E3. Duplicate tax code sau khi sửa
- Báo lỗi

## 10.3.12 Request model đề xuất
```json
{
  "companyName": "ABC Steel Construction JSC",
  "address": "Ho Chi Minh",
  "contactPerson": "Tran Van B",
  "phone": "0908888888",
  "email": "newcontact@abcsteel.vn",
  "customerType": "DISTRIBUTOR",
  "priceGroup": "DISTRIBUTOR",
  "creditLimit": 800000000,
  "paymentTerms": "50% advance, 50% after delivery"
}
```

## 10.3.13 Response model đề xuất
```json
{
  "message": "Customer updated successfully"
}
```

---

# 10.4 CM-04 Disable Customer

## 10.4.1 Mục tiêu
Cho phép Accountant vô hiệu hóa customer mà vẫn giữ nguyên lịch sử dữ liệu.

## 10.4.2 Actor
- Primary actor: Accountant

## 10.4.3 Tiền điều kiện
- Accountant đã đăng nhập
- Customer tồn tại
- Customer đang `ACTIVE`

## 10.4.4 Hậu điều kiện
- `customers.status = INACTIVE`
- Nếu customer có portal login, có thể đồng bộ disable `users.status = INACTIVE`
- Customer không được tạo giao dịch mới
- Dữ liệu lịch sử vẫn còn nguyên

## 10.4.5 Rule nghiệp vụ theo spec
- không được disable customer nếu outstanding debts > 100,000 VND
- không được disable nếu có active projects hoặc open orders
- reactivation cần review/approval
- disabled customer bị loại khỏi các luồng marketing communication

## 10.4.6 Main flow
1. Accountant chọn customer active
2. Hệ thống hiển thị warning / impact analysis
3. Accountant chọn disable reason
4. Hệ thống kiểm tra:
   - debt
   - active projects
   - open orders/contracts nếu policy yêu cầu
5. Nếu pass rule, cập nhật `customers.status = INACTIVE`
6. Nếu có linked portal user, cập nhật `users.status = INACTIVE`
7. Ghi audit log
8. Trả thành công

## 10.4.7 Alternative flow
### A1. Temporary disable
- Spec nhắc như hướng mở rộng
- schema hiện tại chưa có trạng thái riêng

### A2. Merge before disable
- Ngoài phạm vi schema hiện tại

## 10.4.8 Exception flow
### E1. Outstanding debt vượt ngưỡng
- từ chối disable

### E2. Active projects hoặc open orders còn tồn tại
- từ chối disable

### E3. Customer đã INACTIVE
- trả thông báo phù hợp, không update lại

## 10.4.9 Request model đề xuất
```json
{
  "reason": "Customer inactive for long period"
}
```

## 10.4.10 Response model đề xuất
```json
{
  "message": "Customer disabled successfully"
}
```

---

## 11. API đề xuất cho module

| API ID | API Name | Method | Endpoint |
|---|---|---|---|
| CM-API-01 | Create Customer | POST | /api/customers |
| CM-API-02 | Get Customer List | GET | /api/customers |
| CM-API-03 | Get Customer Detail | GET | /api/customers/{id} |
| CM-API-04 | Update Customer | PUT | /api/customers/{id} |
| CM-API-05 | Disable Customer | PATCH | /api/customers/{id}/disable |
| CM-API-06 | Get Customer Summary | GET | /api/customers/{id}/summary |

### Lưu ý
Spec gốc không tách rõ list API, nhưng backend thực tế gần như chắc chắn cần:
- list customers
- detail customer
- summary/debt-related view

---

## 12. Validation tổng hợp theo API

## 12.1 Create Customer
- `companyName` required
- `taxCode` required, unique, 10–13 digits
- `customerType` required
- `creditLimit >= 0`
- `email` valid nếu có

## 12.2 Update Customer
- customer tồn tại
- các field restricted phải được kiểm tra riêng
- tax code nếu sửa thì vẫn unique
- creditLimit hợp lệ

## 12.3 Disable Customer
- customer tồn tại
- status hiện tại = ACTIVE
- debt <= threshold cho phép
- không có active projects / open orders theo rule

---

## 13. Yêu cầu transaction

Các luồng sau phải chạy transaction:

### Create Customer
1. validate dữ liệu
2. insert `customers`
3. optional insert `users`
4. insert `audit_logs`
5. commit

Nếu lỗi bất kỳ bước nào:
- rollback toàn bộ

### Update Customer
1. validate dữ liệu
2. update `customers`
3. optional update `users`
4. insert `audit_logs`
5. commit

### Disable Customer
1. check dependencies
2. update `customers.status`
3. optional update `users.status`
4. insert `audit_logs`
5. commit

---

## 14. Yêu cầu audit log

Các action nên được log:

- `CREATE_CUSTOMER`
- `UPDATE_CUSTOMER`
- `DISABLE_CUSTOMER`
- `VIEW_CUSTOMER_DETAIL` nếu policy muốn audit record views

### Ví dụ log update customer
```json
{
  "action": "UPDATE_CUSTOMER",
  "entityType": "CUSTOMER",
  "entityId": "customer-uuid",
  "oldValue": {
    "companyName": "ABC Steel Construction Co., Ltd",
    "creditLimit": 500000000
  },
  "newValue": {
    "companyName": "ABC Steel Construction JSC",
    "creditLimit": 800000000
  }
}
```

---

## 15. Cách tính outstanding debt đề xuất

Schema hiện tại không có bảng `debts` snapshot rõ ràng, nên outstanding debt có thể suy luận từ:

- các invoice của customer
- trừ đi payment allocations

### Công thức logic
`Outstanding Debt = Tổng invoice amount chưa thanh toán - tổng allocations đã ghi nhận`

### Dữ liệu cần dùng
- `invoices.customer_id`
- `invoices.total_amount`
- `invoices.vat_amount`
- `payment_allocations.invoice_id`
- `payments.customer_id`

### Lưu ý
Cần thống nhất công thức:
- có cộng VAT vào debt hay không
- invoice status nào được tính vào outstanding

---

## 16. Error handling đề xuất

### Nhóm lỗi dữ liệu
- Missing required field
- Invalid tax code
- Duplicate tax code
- Invalid email format
- Credit limit invalid
- Customer not found
- Customer already inactive

### Nhóm lỗi nghiệp vụ
- Cannot disable customer with outstanding debt
- Cannot disable customer with active projects
- Cannot modify restricted field
- Active orders prevent update/disable

### Nhóm lỗi hệ thống
- Unexpected error
- Database transaction failed

---

## 17. Khoảng trống giữa spec và database hiện tại

Có một số điểm chênh cần lưu ý:

### 17.1 Thiếu `customer_code`
Spec nêu:
- customer code format `CUST-XXX sequential`

Schema `customers` chưa có cột:
- `customer_code`

### 17.2 Thiếu `price_group`
Spec có price group, nhưng bảng `customers` chưa có:
- `price_group`

Hiện tại có thể phải suy từ `customer_type`, nhưng không hoàn toàn tương đương.

### 17.3 Thiếu `payment_terms`
Spec có payment terms của customer, nhưng DB chưa có cột:
- `payment_terms`

### 17.4 Thiếu multiple contact persons
Spec có `Contact Persons`, nhưng DB hiện chỉ có:
- `contact_person`

### 17.5 Thiếu documents
Spec có khu vực documents, nhưng schema chưa có bảng document/customer_document

### 17.6 Thiếu soft reactivation workflow
Spec nhắc reactivation cần review/approval, nhưng DB chưa có:
- approval workflow fields
- reactivation request tracking

### 17.7 Thiếu dedicated debt table
Schema hiện đủ để suy debt bằng query tổng hợp, nhưng chưa có bảng snapshot/summary để truy vấn nhanh

---

## 18. Đề xuất cải tiến schema nếu muốn bám spec sát hơn

### 18.1 Bổ sung customer_code, price_group, payment_terms
```sql
ALTER TABLE customers
ADD COLUMN customer_code VARCHAR(50) UNIQUE NULL,
ADD COLUMN price_group VARCHAR(50) NULL,
ADD COLUMN payment_terms VARCHAR(255) NULL;
```

### 18.2 Bổ sung nhiều contact persons
```sql
CREATE TABLE customer_contacts (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    position VARCHAR(100),
    is_primary BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_customer_contact_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

### 18.3 Bổ sung document table
```sql
CREATE TABLE customer_documents (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    document_type VARCHAR(100),
    file_name VARCHAR(255),
    file_url VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_document_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

### 18.4 Bổ sung credit/debt snapshot nếu cần
```sql
ALTER TABLE customers
ADD COLUMN current_outstanding_debt DECIMAL(18,2) DEFAULT 0;
```

### Ghi chú
Nếu muốn giữ normalized tốt hơn, có thể không thêm snapshot field mà dùng materialized view / reporting query.

---

## 19. Acceptance criteria tổng hợp

Module Customer Management được xem là đáp ứng yêu cầu khi:

1. Accountant tạo được customer mới
2. Hệ thống chặn duplicate tax code
3. Tax code được validate đúng định dạng
4. Accountant xem được customer detail
5. Accountant cập nhật được customer information
6. Các field bị hạn chế được kiểm tra đúng rule
7. Không thể disable customer nếu còn debt vượt ngưỡng
8. Không thể disable customer nếu còn active projects / open orders
9. Disable customer không xóa dữ liệu lịch sử
10. Nếu có portal account liên kết, status được đồng bộ hợp lý
11. Các thay đổi quan trọng có audit log

---

## 20. Test scenarios cốt lõi

### TC01 – Create customer thành công
- Accountant active
- Tax code hợp lệ, chưa tồn tại
- Kỳ vọng: tạo `customers` thành công

### TC02 – Create customer tax code trùng
- tax code đã tồn tại
- Kỳ vọng: báo lỗi duplicate

### TC03 – Create customer tax code sai định dạng
- tax code không đủ 10–13 chữ số
- Kỳ vọng: báo lỗi validation

### TC04 – View customer detail thành công
- customer tồn tại
- Kỳ vọng: trả đúng profile detail

### TC05 – Update customer thành công
- sửa address/contact/creditLimit hợp lệ
- Kỳ vọng: update thành công

### TC06 – Update customer đổi tax code sang giá trị đã tồn tại
- Kỳ vọng: báo lỗi duplicate

### TC07 – Disable customer thành công
- customer active
- không có debt vượt ngưỡng
- không có active projects/open orders
- Kỳ vọng: status = INACTIVE

### TC08 – Disable customer còn debt
- outstanding debt > 100,000 VND
- Kỳ vọng: bị chặn

### TC09 – Disable customer còn active project
- Kỳ vọng: bị chặn

### TC10 – Disable customer đã inactive
- Kỳ vọng: trả thông báo phù hợp

---

## 21. Kết luận

Module **Customer Management** là nền tảng của hầu hết các quy trình nghiệp vụ trong G90.

Với schema hiện tại, hệ thống đã đủ cơ sở để triển khai phiên bản cơ bản của module này với:
- `customers`
- `users`
- `projects`
- `quotations`
- `contracts`
- `invoices`
- `payments`
- `audit_logs`

Tuy nhiên, để bám spec sát hơn và triển khai trọn vẹn hơn, nên bổ sung:
- `customer_code`
- `price_group`
- `payment_terms`
- bảng `customer_contacts`
- bảng `customer_documents`
- cơ chế debt summary rõ ràng hơn

Nếu triển khai tiếp theo, module hợp lý nhất để nối với Customer Management là:
- **Project Construction Management**
- hoặc **View Debt Status / Debt Management**
vì đây là hai hướng sử dụng dữ liệu customer trực tiếp và thường xuyên nhất.
