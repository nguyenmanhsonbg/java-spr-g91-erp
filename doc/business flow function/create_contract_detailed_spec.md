# Đặc tả chi tiết luồng Tạo contract

## 1. Thông tin tài liệu

- **Tài liệu:** Detailed Specification – Create Contract Flow
- **Project:** G90 Steel Business Management System
- **Module:** Contract Management
- **Use case:** Create Contract
- **Primary actor:** Accountant
- **Phạm vi:** Đặc tả nghiệp vụ, dữ liệu, validation, trạng thái, API đề xuất, mapping database, business rules và khoảng trống giữa spec với schema hiện tại cho luồng tạo contract
- **Nguồn tham chiếu:** Project Spec (`Spec_Project_G90.md`), Database Schema (`V1__init.sql`)

---

## 2. Mục tiêu nghiệp vụ

Cho phép **Accountant** tạo một **contract bán hàng** cho khách hàng, đặc biệt cho các kênh offline hoặc trường hợp cần xử lý nội bộ thay vì để customer tự thao tác toàn bộ.

Luồng này giúp hệ thống:

- chuẩn hóa dữ liệu hợp đồng bán hàng
- áp dụng giá theo chính sách khách hàng
- hỗ trợ chọn dữ liệu từ quotation có sẵn hoặc tạo contract trực tiếp
- kiểm soát hạn mức tín dụng và dư nợ của khách hàng
- lưu điều khoản thanh toán và địa chỉ giao hàng
- chuẩn bị dữ liệu cho các bước tiếp theo như approval, submit, inventory reservation, invoicing

---

## 3. Phạm vi nghiệp vụ

### 3.1 Trong phạm vi
- Accountant chọn customer
- Hệ thống hiển thị thông tin tín dụng / debt summary liên quan
- Accountant thêm sản phẩm và số lượng
- Hệ thống lấy đơn giá theo price policy
- Accountant có thể nhập đơn giá cuối cùng trong biên độ cho phép
- Accountant nhập payment terms
- Accountant nhập delivery address
- Accountant có thể tạo từ quotation hoặc tạo mới trực tiếp
- Hệ thống tính tổng tiền hợp đồng
- Hệ thống lưu contract và contract items
- Hệ thống gán trạng thái khởi tạo

### 3.2 Ngoài phạm vi
- Duyệt contract
- Submit contract
- Cancel contract
- Track contract
- Print contract
- Invoice creation
- Inventory issue thực tế
- Payment recording

---

## 4. Actor và phân quyền

### 4.1 Actor chính
- **Accountant**

### 4.2 Actor liên quan
- **Owner**: có thể approve contract trong các trường hợp yêu cầu phê duyệt
- **Customer**: là đối tượng của contract, có thể xem contract sau khi được tạo theo đúng quyền
- **Warehouse**: nhận thông tin sau các bước submit/reservation
- **System**: tính giá, validate rule, tạo mã contract, lưu dữ liệu, ghi audit log

### 4.3 Quyền truy cập
- Chỉ **Accountant** được dùng luồng Create Contract
- Owner không phải actor chính của use case này
- Customer không trực tiếp tạo contract trong flow này
- Contract tạo ra phải thuộc một customer xác định
- Accountant được phép tạo contract cho mọi customer hợp lệ theo quyền nội bộ của hệ thống

---

## 5. Tiền điều kiện

Luồng tạo contract chỉ được thực hiện khi thỏa các điều kiện sau:

1. User đã đăng nhập
2. User có role = `ACCOUNTANT`
3. Tài khoản người dùng đang `ACTIVE`
4. Customer tồn tại và đang hợp lệ cho giao dịch
5. Có ít nhất một sản phẩm active
6. Có pricing data phù hợp cho customer
7. Nếu tạo từ quotation thì quotation tồn tại và có thể dùng làm nguồn
8. Nếu có project liên quan thì project phải hợp lệ và traceable theo customer nếu business cần

---

## 6. Hậu điều kiện

### 6.1 Khi thành công
- Một bản ghi mới được tạo trong `contracts`
- Nhiều bản ghi được tạo trong `contract_items`
- `contract_number` được sinh tự động
- `status` được gán trạng thái ban đầu
- Tổng tiền được lưu trong `contracts.total_amount`
- Contract sẵn sàng cho các bước tiếp theo như approval / submit

### 6.2 Khi thất bại
- Không lưu dữ liệu một phần
- Toàn bộ transaction phải rollback
- Trả lỗi phù hợp với từng tình huống nghiệp vụ hoặc hệ thống

---

## 7. Trigger

Luồng bắt đầu khi Accountant:

- mở màn hình Create Contract
- chọn customer
- thêm sản phẩm hoặc chọn quotation để copy
- nhập các điều khoản cần thiết
- nhấn lưu contract

---

## 8. Dữ liệu đầu vào nghiệp vụ

## 8.1 Header contract
Theo spec và schema hiện tại, phần header có thể bao gồm:

- Customer
- Quotation tham chiếu (optional)
- Payment terms
- Delivery address
- Trạng thái
- Người tạo
- Ngày tạo

### Dữ liệu nên có ở nghiệp vụ nhưng schema hiện tại chưa đủ đầy
- Credit check snapshot
- Current debt snapshot
- Delivery terms chi tiết
- Ghi chú
- Approval flag / approval status
- Deposit requirement
- Project reference trực tiếp ở contract

## 8.2 Danh sách line item
Mỗi dòng gồm:
- Product ID
- Product code
- Product name
- Type
- Size
- Thickness
- Quantity
- Unit price
- Total price

---

## 9. Luồng nghiệp vụ chính

### Bước 1: Accountant mở form tạo contract
Hệ thống hiển thị:
- ô chọn customer
- tùy chọn copy từ quotation
- danh sách sản phẩm
- payment terms
- delivery address
- các trường pricing / quantity / total

### Bước 2: Accountant chọn customer
Khi customer được chọn, hệ thống hiển thị thông tin quan trọng theo spec:
- credit limit
- current debt
- payment terms mặc định nếu có
- phân loại customer / price group tương ứng

### Bước 3: Accountant chọn nguồn tạo contract
Có 2 nhánh chính:

#### Cách A: Tạo trực tiếp
- Accountant tự chọn products
- tự nhập quantity
- hệ thống tính và hiển thị giá

#### Cách B: Copy từ quotation
- Accountant chọn quotation có sẵn
- hệ thống nạp customer, items, giá và thông tin liên quan
- Accountant có thể chỉnh sửa trong phạm vi cho phép

### Bước 4: Accountant thêm items
Accountant chọn một hoặc nhiều sản phẩm.

Hệ thống kiểm tra:
- product phải tồn tại
- product phải active
- quantity > 0

### Bước 5: Hệ thống xác định đơn giá cơ sở
System dựa trên:
- customer
- customer group / customer type
- bảng giá còn hiệu lực
- price list item theo product

### Bước 6: Accountant nhập / điều chỉnh đơn giá cuối cùng
Theo spec:
- Accountant có thể thương lượng và nhập final unit price trong biên độ cho phép

Điều này dẫn tới 2 lớp giá:
- **Base price**: giá hệ thống lấy theo chính sách
- **Final negotiated price**: giá lưu trong contract item

### Bước 7: Hệ thống validate giá
Các kiểm tra:
- final unit price không được thấp hơn mức tối thiểu cho phép theo policy
- nếu giảm giá vượt ngưỡng cho phép thì cần approval
- nếu policy yêu cầu, phải ghi nhận lý do điều chỉnh

### Bước 8: Accountant nhập payment terms và delivery address
Theo spec:
- payment terms là bắt buộc ở mức nghiệp vụ
- delivery address là bắt buộc cho contract giao hàng

### Bước 9: Hệ thống tính tổng hợp đồng
- `line_total = quantity × unit_price`
- `contract.total_amount = sum(line_total)`

### Bước 10: Hệ thống kiểm tra credit/debt/inventory
Theo spec:
- hiển thị credit limit và current debt
- có thể báo lỗi nếu vượt credit limit
- có thể báo lỗi nếu inventory không đủ

### Bước 11: Hệ thống quyết định trạng thái khởi tạo
Theo spec:
- save contract with `status = DRAFT`

Nếu contract vượt ngưỡng approval hoặc giảm giá đặc biệt:
- vẫn có thể lưu `DRAFT`
- các bước approval diễn ra ở flow khác

### Bước 12: Lưu contract
Khi Accountant xác nhận:
- hệ thống sinh `contract_number`
- insert `contracts`
- insert `contract_items`
- ghi audit log
- trả kết quả thành công

---

## 10. Luồng thay thế

### A1. Tạo contract từ quotation
- Accountant chọn một quotation hợp lệ
- hệ thống map `quotation_id`
- copy items sang contract
- cho phép chỉnh sửa trong phạm vi chính sách

### A2. Contract cần approval do giá trị lớn
Spec nêu:
- contract > 500M VND có thể cần owner approval

Hành vi đề xuất:
- vẫn lưu được contract với `status = DRAFT` hoặc `PENDING_APPROVAL`
- flow approve sẽ xử lý sau

**Lưu ý:** schema hiện tại chưa có cột `approval_status`

### A3. Contract cần approval do discount vượt biên độ
- final unit price thấp hơn base price vượt mức cho phép
- đánh dấu contract cần duyệt

### A4. Tạo contract cho khách hàng mới
Spec có rule deposit khác nhau cho:
- khách hàng mới
- khách hàng có lịch sử 6+ tháng

Schema hiện tại chưa có trường trực tiếp để xác định age bucket của customer ngoài `created_at`, nên có thể suy ra từ `customers.created_at`

---

## 11. Luồng ngoại lệ / lỗi nghiệp vụ

### E1. Customer không tồn tại
- chặn tạo contract

### E2. Không có pricing hợp lệ
- không tìm được bảng giá cho product/customer
- chặn lưu contract

### E3. Vượt credit limit
Theo spec:
- hiển thị credit limit và current debt
- có thể chặn hoặc yêu cầu approval tùy policy
- với bản đặc tả an toàn này, đề xuất **chặn tạo hoặc đánh dấu cần review**

### E4. Inventory không đủ
Spec nêu đây là exception có thể xảy ra.

Lưu ý:
- create contract là bước sớm hơn submit/reservation
- nếu hệ thống muốn cho tạo contract ngay cả khi chưa đủ tồn thì cần rule rõ
- đề xuất:
  - tại create contract: cảnh báo
  - tại submit contract: chặn cứng nếu inventory không đủ

### E5. Final unit price dưới mức tối thiểu
- chặn lưu hoặc lưu trạng thái chờ duyệt tùy policy

### E6. Quotation không hợp lệ để copy
Ví dụ:
- quotation không tồn tại
- quotation không thuộc customer được chọn
- quotation đã bị vô hiệu hóa theo policy

### E7. Lỗi transaction database
- rollback toàn bộ contract

---

## 12. Business rules áp dụng

Các business rules quan trọng của luồng này:

1. Chỉ Accountant được tạo contract
2. Contract có thể được tạo trực tiếp hoặc từ quotation
3. Hệ thống phải hiển thị credit limit và current debt của customer
4. Contract lưu khởi tạo với trạng thái `DRAFT`
5. Giá contract không được thấp hơn base price theo policy
6. Các thay đổi giá vượt biên độ cần approval
7. Contract > 500M VND cần approval
8. Payment terms chuẩn có thể là 70% khi giao hàng, 30% trong 30 ngày
9. Deposit requirement:
   - 30% cho khách hàng mới
   - 20% cho khách hàng có lịch sử >= 6 tháng
10. Các action quan trọng phải ghi audit log
11. Money unit dùng VND
12. Contract phải gắn với đúng customer

---

## 13. Dữ liệu và mapping database

## 13.1 Bảng sử dụng

### `users`
Dùng để xác định user hiện tại và role.

Cột liên quan:
- `id`
- `role_id`
- `status`

### `customers`
Dùng để xác định hồ sơ khách hàng.

Cột liên quan:
- `id`
- `company_name`
- `customer_type`
- `credit_limit`
- `status`
- `created_at`

### `products`
Dùng để chọn item hợp đồng.

Cột liên quan:
- `id`
- `product_code`
- `product_name`
- `type`
- `size`
- `thickness`
- `unit`
- `status`

### `price_lists`
Dùng để xác định bảng giá áp dụng.

Cột liên quan:
- `id`
- `customer_group`
- `start_date`
- `end_date`
- `status`

### `price_list_items`
Dùng để lấy base unit price.

Cột liên quan:
- `id`
- `price_list_id`
- `product_id`
- `unit_price`

### `quotations`
Dùng làm nguồn nếu copy từ quotation.

Cột liên quan:
- `id`
- `quotation_number`
- `customer_id`
- `total_amount`
- `status`
- `valid_until`

### `quotation_items`
Dùng để copy items nếu tạo từ quotation.

### `contracts`
Header contract.

Cột map hiện có:
- `id`
- `contract_number`
- `customer_id`
- `quotation_id`
- `total_amount`
- `status`
- `payment_terms`
- `delivery_address`
- `created_by`
- `created_at`

### `contract_items`
Chi tiết contract item.

Cột map:
- `id`
- `contract_id`
- `product_id`
- `quantity`
- `unit_price`
- `total_price`

### `inventory`
Dùng để tham chiếu tồn kho hiện tại nếu hệ thống check sớm.

Cột liên quan:
- `warehouse_id`
- `product_id`
- `quantity`

### `payments`
### `payment_allocations`
### `invoices`
Ba nhóm bảng này có thể được dùng để tính current debt sâu hơn nếu hệ thống không có bảng debt summary riêng.

### `audit_logs`
Lưu vết tạo contract.

---

## 14. Cách tính current debt đề xuất

Do schema hiện tại không có bảng `debts` riêng dạng snapshot, current debt có thể suy ra từ:

- tổng invoice outstanding của customer
- hoặc:
  - `sum(invoices.total_amount + invoices.vat_amount nếu cần)`
  - trừ đi `sum(payment_allocations.amount)` trên các invoice của customer chưa tất toán

### Lưu ý
Schema hiện có đủ dữ liệu để suy luận debt, nhưng cần query tổng hợp ở tầng service/reporting.

---

## 15. Mapping request model đề xuất

```json
{
  "customerId": "customer-uuid",
  "quotationId": "optional-quotation-uuid",
  "paymentTerms": "70% on delivery, 30% within 30 days",
  "deliveryAddress": "Cong trinh ABC, Ha Noi",
  "note": "Giao theo 2 dot",
  "items": [
    {
      "productId": "product-uuid-1",
      "quantity": 10,
      "unitPrice": 1500000
    },
    {
      "productId": "product-uuid-2",
      "quantity": 5,
      "unitPrice": 2300000
    }
  ]
}
```

### Ghi chú
- `unitPrice` trong request là final negotiated price
- `note` có ở tầng nghiệp vụ nhưng **schema hiện tại chưa có cột note trong `contracts`**
- nếu tạo từ quotation, có thể cho phép bỏ `items` và hệ thống copy từ quotation

---

## 16. Mapping response model đề xuất

```json
{
  "id": "contract-uuid",
  "contractNumber": "CT-20260314-0001",
  "customerId": "customer-uuid",
  "quotationId": "quotation-uuid",
  "status": "DRAFT",
  "paymentTerms": "70% on delivery, 30% within 30 days",
  "deliveryAddress": "Cong trinh ABC, Ha Noi",
  "totalAmount": 26500000,
  "requiresApproval": false,
  "items": [
    {
      "id": "contract-item-uuid-1",
      "productId": "product-uuid-1",
      "productCode": "HB100",
      "productName": "Steel H Beam",
      "type": "H-BEAM",
      "size": "100x100",
      "thickness": "6",
      "quantity": 10,
      "unitPrice": 1500000,
      "totalPrice": 15000000
    },
    {
      "id": "contract-item-uuid-2",
      "productId": "product-uuid-2",
      "productCode": "PL200",
      "productName": "Steel Plate",
      "type": "PLATE",
      "size": "200x200",
      "thickness": "8",
      "quantity": 5,
      "unitPrice": 2300000,
      "totalPrice": 11500000
    }
  ],
  "createdAt": "2026-03-14T10:15:00+07:00"
}
```

---

## 17. Validation chi tiết

## 17.1 Validation authentication / authorization
- User phải đăng nhập
- Role phải là `ACCOUNTANT`
- Account status phải là `ACTIVE`

## 17.2 Validation customer
- `customerId` bắt buộc khi tạo trực tiếp
- customer phải tồn tại
- customer phải active hoặc ở trạng thái cho phép giao dịch

## 17.3 Validation quotation
Nếu có `quotationId`:
- quotation phải tồn tại
- quotation phải thuộc chính `customerId`
- quotation phải còn dùng được theo policy
- quotation items phải tồn tại

## 17.4 Validation items
- phải có ít nhất 1 item
- mỗi item phải có `productId`
- `quantity > 0`
- `unitPrice > 0`
- không duplicate product tùy policy; nếu duplicate thì merge hoặc chặn

## 17.5 Validation product
- product phải tồn tại
- product phải active

## 17.6 Validation pricing
- cần xác định được base price cho từng product
- final unit price phải tuân theo pricing policy
- nếu thấp hơn ngưỡng cho phép thì contract phải bị chặn hoặc flagged approval

## 17.7 Validation payment terms
- paymentTerms bắt buộc
- không vượt max length hợp lý

## 17.8 Validation delivery address
- deliveryAddress bắt buộc
- không vượt max length hợp lý

## 17.9 Validation total amount
- `totalAmount > 0`

---

## 18. Trạng thái và lifecycle

### 18.1 Trạng thái liên quan
Theo spec và schema có thể dùng:
- `DRAFT`
- `SUBMITTED`
- `APPROVED`
- `CANCELLED`
- `PROCESSING`
- `RESERVED`
- `PICKED`
- `SHIPPED`
- `DELIVERED`
- `COMPLETED`

### 18.2 Trạng thái khởi tạo
- Khi create thành công: `DRAFT`

### 18.3 Lifecycle liên quan
```text
DRAFT -> SUBMITTED -> APPROVED -> PROCESSING -> RESERVED -> PICKED -> SHIPPED -> DELIVERED -> COMPLETED
     -> CANCELLED
```

---

## 19. Approval decision logic đề xuất

## 19.1 Điều kiện cần approval
Một contract nên được đánh dấu cần approval khi:

1. `total_amount > 500,000,000`
2. final price dưới base price vượt ngưỡng cho phép
3. customer vượt credit limit
4. discount đặc biệt / key customer case theo policy nội bộ

## 19.2 Cách thể hiện với schema hiện tại
Schema hiện chưa có các cột:
- `requires_approval`
- `approval_status`
- `approved_by`
- `approved_at`

### Tạm thời
Có thể:
- lưu `status = DRAFT`
- business layer trả thêm `requiresApproval = true`

### Khuyến nghị
Bổ sung schema để support approval đúng nghĩa

---

## 20. Yêu cầu transaction

Luồng tạo contract phải chạy trong **một database transaction**.

Trình tự đề xuất:
1. Validate toàn bộ dữ liệu đầu vào
2. Tính toán pricing / total / approval flags
3. Insert `contracts`
4. Insert `contract_items`
5. Insert `audit_logs`
6. Commit

Nếu lỗi ở bất kỳ bước nào:
- rollback toàn bộ

---

## 21. Yêu cầu audit log

Khi tạo contract thành công, hệ thống phải ghi log tối thiểu:

- `action = CREATE_CONTRACT`
- `entity_type = CONTRACT`
- `entity_id = contracts.id`
- `user_id = accountant hiện tại`
- `new_value` chứa snapshot cơ bản của contract

Ví dụ:

```json
{
  "action": "CREATE_CONTRACT",
  "entityType": "CONTRACT",
  "entityId": "contract-uuid",
  "newValue": {
    "contractNumber": "CT-20260314-0001",
    "customerId": "customer-uuid",
    "status": "DRAFT",
    "totalAmount": 26500000
  }
}
```

---

## 22. Yêu cầu API đề xuất

## 22.1 API preview contract pricing
### Endpoint
`POST /api/contracts/preview`

### Mục đích
- tính giá
- check base price
- check approval flag
- chưa lưu DB

### Request
```json
{
  "customerId": "customer-uuid",
  "quotationId": "optional-quotation-uuid",
  "paymentTerms": "70% on delivery, 30% within 30 days",
  "deliveryAddress": "Cong trinh ABC, Ha Noi",
  "items": [
    {
      "productId": "product-uuid-1",
      "quantity": 10,
      "unitPrice": 1500000
    }
  ]
}
```

### Response
- total amount
- item totals
- base price vs final price
- requiresApproval
- credit/debt warnings

## 22.2 API create contract
### Endpoint
`POST /api/contracts`

### Mục đích
- tạo contract chính thức
- lưu DB

### Request
```json
{
  "customerId": "customer-uuid",
  "quotationId": "optional-quotation-uuid",
  "paymentTerms": "70% on delivery, 30% within 30 days",
  "deliveryAddress": "Cong trinh ABC, Ha Noi",
  "note": "Giao theo 2 dot",
  "items": [
    {
      "productId": "product-uuid-1",
      "quantity": 10,
      "unitPrice": 1500000
    }
  ]
}
```

### Response
- trả contract header
- trả items
- trả status
- trả requiresApproval

## 22.3 API create contract from quotation
### Endpoint
`POST /api/contracts/from-quotation`

### Mục đích
- tạo contract từ quotation
- có thể copy dữ liệu tự động

---

## 23. Pseudo workflow xử lý backend

```text
1. Lấy user hiện tại từ security context
2. Kiểm tra role ACCOUNTANT
3. Validate customer
4. Nếu có quotationId -> load quotation và validate ownership consistency
5. Validate items
6. Load products
7. Load active price list theo customer group
8. Tính base price cho từng item
9. So final price với pricing policy
10. Tính line total và contract total
11. Tính debt / credit exposure
12. Xác định requiresApproval
13. Sinh contract number
14. Insert contracts
15. Insert contract_items
16. Ghi audit log
17. Trả response
```

---

## 24. Khoảng trống giữa spec và database hiện tại

### 24.1 Chưa có approval fields
Spec có approval flow nhưng DB chưa có:
- `requires_approval`
- `approval_status`
- `approved_by`
- `approved_at`

### 24.2 Chưa có note / delivery terms chi tiết
DB `contracts` hiện chỉ có:
- `payment_terms`
- `delivery_address`

Chưa có:
- `note`
- `delivery_terms`
- `special_terms`

### 24.3 Chưa có project reference trực tiếp ở contract
Spec liên kết project và business flow khá chặt, nhưng bảng `contracts` chưa có:
- `project_id`

### 24.4 Chưa có giá cơ sở / giá negotiated tách riêng
`contract_items` hiện chỉ có:
- `unit_price`
- `total_price`

Chưa có:
- `base_unit_price`
- `discount_percent`
- `pricing_rule_id`

### 24.5 Chưa có snapshot credit/debt
Nếu muốn audit tốt hơn tại thời điểm tạo contract, nên lưu snapshot:
- `credit_limit_at_create`
- `current_debt_at_create`

### 24.6 Chưa có inventory reservation relation
Create contract là bước sớm, nhưng nếu cần trace rõ hơn thì sau này có thể bổ sung order/reservation model riêng

---

## 25. Đề xuất cải tiến schema nếu muốn bám spec sát hơn

### 25.1 Bổ sung thông tin approval
```sql
ALTER TABLE contracts
ADD COLUMN requires_approval BOOLEAN DEFAULT FALSE,
ADD COLUMN approval_status VARCHAR(30) NULL,
ADD COLUMN approved_by CHAR(36) NULL,
ADD COLUMN approved_at TIMESTAMP NULL;
```

### 25.2 Bổ sung note và project
```sql
ALTER TABLE contracts
ADD COLUMN project_id CHAR(36) NULL,
ADD COLUMN note VARCHAR(1000) NULL;
```

### 25.3 Bổ sung base price snapshot
```sql
ALTER TABLE contract_items
ADD COLUMN base_unit_price DECIMAL(18,2) NULL,
ADD COLUMN discount_amount DECIMAL(18,2) DEFAULT 0;
```

### 25.4 Ràng buộc unique contract number
```sql
ALTER TABLE contracts
ADD CONSTRAINT uk_contract_number UNIQUE (contract_number);
```

---

## 26. Tiêu chí chấp nhận nghiệp vụ

Một contract được xem là tạo thành công khi:

1. Accountant hợp lệ và đang active
2. Customer tồn tại và hợp lệ
3. Có ít nhất 1 item
4. Tất cả products đều active
5. Tất cả items có giá hợp lệ
6. paymentTerms và deliveryAddress hợp lệ
7. Header và items được lưu đầy đủ
8. `status = DRAFT`
9. Tổng tiền được tính đúng
10. Có audit log
11. Nếu vượt ngưỡng approval thì hệ thống đánh dấu được cần duyệt ở tầng nghiệp vụ

---

## 27. Test scenarios cốt lõi

### TC01 – Tạo contract trực tiếp thành công
- Accountant active
- Customer active
- 2 products active
- Giá hợp lệ
- Kỳ vọng: tạo contract `DRAFT`

### TC02 – Tạo contract từ quotation thành công
- quotation hợp lệ
- customer khớp
- Kỳ vọng: copy dữ liệu và tạo contract

### TC03 – Customer không tồn tại
- Kỳ vọng: báo lỗi, không lưu DB

### TC04 – Không có pricing hợp lệ
- Kỳ vọng: báo lỗi

### TC05 – Final price dưới ngưỡng cho phép
- Kỳ vọng: chặn hoặc đánh dấu cần approval

### TC06 – Contract > 500M
- Kỳ vọng: contract được tạo và flagged approval theo policy

### TC07 – Delivery address rỗng
- Kỳ vọng: chặn lưu

### TC08 – Payment terms rỗng
- Kỳ vọng: chặn lưu

### TC09 – Lỗi insert contract item
- Header insert thành công, item insert lỗi
- Kỳ vọng: rollback toàn bộ

### TC10 – Quotation không thuộc customer
- Kỳ vọng: chặn tạo từ quotation

---

## 28. Kết luận

Luồng **Tạo contract** là một bước trung tâm trong nghiệp vụ thương mại của G90, kết nối trực tiếp giữa:

- customer
- quotation
- pricing policy
- debt / credit control
- approval
- fulfillment pipeline

Schema hiện tại đã đủ để triển khai **phiên bản cơ bản** của flow này với:
- `contracts`
- `contract_items`
- `quotations`
- `price_lists`
- `price_list_items`

Tuy nhiên, để bám spec sát hơn và đủ cho vận hành thực tế, nên bổ sung:
- approval fields
- project reference trên contract
- note / delivery terms chi tiết
- base price snapshot
- credit/debt snapshot
- contract number unique constraint
