# Đặc tả chi tiết luồng Tạo quotation

## 1. Thông tin tài liệu

- **Tài liệu:** Detailed Specification – Create Quotation Flow
- **Project:** G90 Steel Business Management System
- **Module:** Contract Management / Quotation Management
- **Use case:** Create Quotation
- **Primary actor:** Customer
- **Phạm vi:** Đặc tả nghiệp vụ, dữ liệu, validation, trạng thái, API đề xuất, mapping database và các rule xử lý cho luồng tạo quotation
- **Nguồn tham chiếu:** Project Spec (`Spec_Project_G90.md`), Database Schema (`V1__init.sql`)

---

## 2. Mục tiêu nghiệp vụ

Cho phép **Customer** đã đăng nhập tạo một **quotation** chính thức để yêu cầu báo giá cho các sản phẩm thép theo **giá cá nhân hóa / customer group pricing**, có thể gắn với một **project**, nhập yêu cầu giao hàng, xem trước tổng tiền và gửi quotation sang hệ thống để **Accountant** tiếp nhận xử lý.

Luồng này phục vụ các mục tiêu:

- Chuẩn hóa yêu cầu báo giá từ khách hàng
- Tính đúng đơn giá theo chính sách giá hiện hành
- Kiểm soát số lượng dòng hàng, giá trị tối thiểu và thời hạn hiệu lực
- Tạo dữ liệu đầu vào để chuyển tiếp sang quy trình contract / order nội bộ

---

## 3. Phạm vi nghiệp vụ

### 3.1 Trong phạm vi
- Customer chọn sản phẩm để báo giá
- Customer nhập số lượng
- Customer có thể chọn project liên quan
- Hệ thống lấy đơn giá theo customer pricing
- Hệ thống tính tổng tiền quotation
- Hệ thống kiểm tra điều kiện hợp lệ trước khi submit
- Hệ thống lưu quotation và quotation items
- Hệ thống gán trạng thái ban đầu
- Hệ thống thiết lập ngày hết hiệu lực
- Hệ thống ghi log phục vụ audit

### 3.2 Ngoài phạm vi
- Duyệt quotation
- Chuyển quotation sang contract
- Đàm phán giá thủ công bởi Accountant
- Thanh toán
- Giao hàng
- In quotation PDF hoàn chỉnh
- Approval nội bộ cho contract

---

## 4. Actor và phân quyền

### 4.1 Actor chính
- **Customer**

### 4.2 Actor liên quan
- **Accountant**: tiếp nhận quotation sau khi khách hàng submit
- **System**: tính giá, validate rule, lưu dữ liệu, tạo mã quotation, log hành động

### 4.3 Quyền truy cập
- Chỉ **Customer đã đăng nhập** mới được tạo quotation
- Customer chỉ được tạo quotation cho **chính mình**
- Customer chỉ được chọn **project thuộc customer đó** nếu hệ thống cho phép gắn project
- Guest không được truy cập luồng này
- Accountant không dùng use case này theo đúng vai trò gốc trong spec

---

## 5. Tiền điều kiện

Luồng tạo quotation chỉ được thực hiện khi đồng thời thỏa các điều kiện sau:

1. User đã đăng nhập thành công
2. User có role = `CUSTOMER`
3. Tài khoản customer đang ở trạng thái hoạt động
4. Customer profile tồn tại trong bảng `customers`
5. Có ít nhất một sản phẩm đang hoạt động trong catalog
6. Có price list hợp lệ áp dụng cho customer group tại thời điểm tạo quotation
7. Nếu chọn project thì project phải tồn tại và thuộc customer hiện tại

---

## 6. Hậu điều kiện

### 6.1 Khi thành công
- Một bản ghi mới được tạo trong bảng `quotations`
- Nhiều bản ghi được tạo trong bảng `quotation_items`
- `quotation_number` được sinh tự động
- `status` được gán trạng thái ban đầu
- `valid_until` được tính = ngày tạo + 15 ngày lịch
- Tổng tiền được lưu tại `quotations.total_amount`
- Dữ liệu sẵn sàng để Accountant xem ở danh sách quotation

### 6.2 Khi thất bại
- Không được lưu một phần dữ liệu
- Toàn bộ transaction phải rollback
- Trả về thông báo lỗi rõ ràng theo từng tình huống

---

## 7. Trigger

Luồng bắt đầu khi Customer tại màn hình quotation:

- chọn nút **Tạo quotation**
- thêm sản phẩm vào danh sách báo giá
- nhấn **Xem trước**
- nhấn **Gửi quotation**

---

## 8. Dữ liệu đầu vào nghiệp vụ

### 8.1 Header quotation
- Customer: lấy ngầm từ phiên đăng nhập
- Project tham chiếu: tùy chọn
- Ngày tạo: hệ thống tự sinh
- Ngày hết hiệu lực: hệ thống tự tính
- Trạng thái: hệ thống gán
- Ghi chú/yêu cầu giao hàng: nên có ở tầng nghiệp vụ, nhưng **chưa có cột riêng trong database hiện tại**
- Áp dụng promotion: spec có nhắc là alternative flow, nhưng **database hiện tại chưa có cấu trúc liên kết promotion vào quotation**

### 8.2 Danh sách line item
Mỗi dòng hàng gồm:
- Product ID
- Product code
- Product name
- Type
- Size
- Thickness
- Unit
- Quantity
- Unit price
- Total price

---

## 9. Luồng nghiệp vụ chính

### Bước 1: Customer mở form tạo quotation
Hệ thống hiển thị:
- thông tin customer hiện tại
- project có thể chọn (nếu có)
- danh sách sản phẩm active
- thông tin spec của sản phẩm
- giá tham khảo hoặc giá áp dụng theo customer group nếu hệ thống cho phép hiện sẵn

### Bước 2: Customer chọn sản phẩm
Customer chọn một hoặc nhiều sản phẩm từ catalog.

Ràng buộc:
- chỉ chọn sản phẩm `ACTIVE`
- không chọn trùng dòng cùng product nhiều lần, hoặc nếu trùng thì hệ thống phải gộp dòng

### Bước 3: Customer nhập số lượng cho từng sản phẩm
Customer nhập quantity cho từng item.

Hệ thống kiểm tra:
- quantity bắt buộc > 0
- quantity phải hợp lệ theo định dạng số
- số dòng hàng tối đa = 20

### Bước 4: Hệ thống xác định đơn giá áp dụng
System dựa trên:
- `customers.customer_type`
- hoặc nhóm khách hàng tương ứng
- `price_lists.customer_group`
- khoảng hiệu lực `start_date`, `end_date`
- bảng `price_list_items`

Hệ thống lấy ra:
- `unit_price` cho từng sản phẩm
- nếu không tìm được giá hợp lệ thì không cho submit quotation

### Bước 5: Hệ thống tính thành tiền từng dòng
Công thức:
- `line_total = quantity × unit_price`

### Bước 6: Hệ thống tính tổng quotation
Công thức:
- `quotation.total_amount = tổng tất cả line_total`

### Bước 7: Hệ thống validate trước khi submit
Kiểm tra toàn bộ:
- customer hợp lệ
- project hợp lệ nếu có
- mọi product hợp lệ
- mọi quantity > 0
- có đủ giá cho tất cả sản phẩm
- tổng giá trị quotation >= 10,000,000 VND
- tổng số line item <= 20

### Bước 8: Customer xem trước quotation
Hệ thống hiển thị:
- header quotation
- danh sách items
- đơn giá
- thành tiền từng dòng
- tổng tiền
- ngày hết hiệu lực dự kiến
- trạng thái sau khi tạo

### Bước 9: Customer submit quotation
Khi customer xác nhận:
- hệ thống sinh `quotation_number`
- lưu `quotations`
- lưu `quotation_items`
- gán `status = PENDING`
- gán `valid_until = created_date + 15 ngày`
- ghi audit log
- trả kết quả thành công

### Bước 10: Hệ thống thông báo cho Accountant
Về mặt nghiệp vụ, spec yêu cầu gửi notification tới Accountant phụ trách.

**Lưu ý:** database hiện tại chưa có bảng notification, queue hoặc mapping accountant assignment, nên đây là yêu cầu nghiệp vụ ở mức hệ thống, chưa được phản ánh đầy đủ trong schema hiện tại.

---

## 10. Luồng thay thế

### A1. Lưu nháp quotation
Spec có nhắc đến khả năng **save as draft**.

Hành vi đề xuất:
- cho phép lưu quotation với `status = DRAFT`
- chưa cần validate ngưỡng tối thiểu hoàn toàn như khi submit, nhưng vẫn cần validate định dạng dữ liệu cơ bản

**Khoảng trống schema hiện tại:**
- bảng `quotations.status` có thể lưu `DRAFT`
- dùng được ngay, không cần thay đổi schema

### A2. Áp dụng promotion
Spec có nhắc “apply promotion” là luồng thay thế.

Hạn chế hiện tại:
- database có `promotions` và `promotion_products`
- nhưng chưa có:
  - `promotion_id` ở quotation
  - `discount_amount`
  - `final_total_amount`
- vì vậy chỉ có thể đặc tả nghiệp vụ ở mức logic, chưa map hoàn chỉnh vào DB hiện tại

### A3. Tạo quotation gắn project
Nếu customer chọn project:
- project phải thuộc customer hiện tại
- `quotations.project_id` được gán tương ứng

**Lưu ý database:** `quotations.project_id` đang có cột nhưng chưa khai báo foreign key tới `projects.id`

---

## 11. Luồng ngoại lệ / lỗi nghiệp vụ

### E1. Không có price list hiệu lực
Điều kiện:
- không có price list phù hợp customer group
- hoặc price list hết hạn
- hoặc không có item giá cho sản phẩm được chọn

Kết quả:
- không cho submit quotation
- hiển thị lỗi: không tìm thấy bảng giá hợp lệ

### E2. Product không còn active
Điều kiện:
- product đã bị deactive / discontinued trong lúc người dùng thao tác

Kết quả:
- loại sản phẩm khỏi quotation
- yêu cầu người dùng chọn lại

### E3. Dòng hàng vượt quá giới hạn
Điều kiện:
- số dòng item > 20

Kết quả:
- chặn submit
- hiển thị lỗi rõ ràng

### E4. Tổng tiền dưới ngưỡng tối thiểu
Điều kiện:
- `total_amount < 10,000,000 VND`

Kết quả:
- chặn submit

### E5. Project không thuộc customer
Điều kiện:
- customer cố tình gửi `projectId` không thuộc quyền sở hữu

Kết quả:
- trả lỗi phân quyền / dữ liệu không hợp lệ

### E6. Lỗi ghi dữ liệu
Điều kiện:
- insert header thành công nhưng insert items lỗi
- hoặc lỗi transaction DB

Kết quả:
- rollback toàn bộ quotation

---

## 12. Business rules áp dụng

Các rule quan trọng của luồng này:

1. Chỉ Customer được tạo quotation
2. Quotation phải dùng giá phù hợp customer pricing
3. Quotation có hiệu lực **15 ngày lịch**
4. Giá trị tối thiểu của quotation = **10,000,000 VND**
5. Tối đa **20 line items**
6. Chỉ product đang active mới được chọn
7. Customer chỉ xem / thao tác quotation của chính mình
8. Mọi thao tác quan trọng phải ghi audit log
9. Nếu ngoài giờ làm việc thì có thể vẫn tạo quotation vì đây là thao tác online; phần xử lý thủ công sẽ để Pending theo rule vận hành chung
10. Các giá trị tiền tệ dùng đơn vị **VND**

---

## 13. Dữ liệu và mapping database

### 13.1 Bảng sử dụng

#### `users`
Dùng để xác định tài khoản đăng nhập và role.

Các cột liên quan:
- `id`
- `role_id`
- `status`
- `email`

#### `customers`
Xác định hồ sơ customer tương ứng với user.

Các cột liên quan:
- `id`
- `user_id`
- `customer_type`
- `status`

#### `products`
Danh mục sản phẩm chọn để báo giá.

Các cột liên quan:
- `id`
- `product_code`
- `product_name`
- `type`
- `size`
- `thickness`
- `unit`
- `status`

#### `price_lists`
Thông tin bảng giá áp dụng.

Các cột liên quan:
- `id`
- `name`
- `customer_group`
- `start_date`
- `end_date`
- `status`

#### `price_list_items`
Đơn giá theo từng sản phẩm trong bảng giá.

Các cột liên quan:
- `id`
- `price_list_id`
- `product_id`
- `unit_price`

#### `projects`
Project được gắn vào quotation nếu có.

Các cột liên quan:
- `id`
- `customer_id`
- `status`

#### `quotations`
Header quotation.

Các cột map:
- `id`
- `quotation_number`
- `customer_id`
- `project_id`
- `total_amount`
- `status`
- `valid_until`
- `created_at`

#### `quotation_items`
Chi tiết từng dòng quotation.

Các cột map:
- `id`
- `quotation_id`
- `product_id`
- `quantity`
- `unit_price`
- `total_price`

#### `audit_logs`
Lưu vết thao tác tạo quotation.

Các cột map:
- `id`
- `user_id`
- `action`
- `entity_type`
- `entity_id`
- `old_value`
- `new_value`
- `created_at`

---

## 14. Mapping request model đề xuất

```json
{
  "projectId": "optional-project-uuid",
  "note": "Giao hàng buổi sáng",
  "deliveryRequirement": "Giao tại công trình",
  "items": [
    {
      "productId": "uuid-product-1",
      "quantity": 10
    },
    {
      "productId": "uuid-product-2",
      "quantity": 5
    }
  ]
}
```

### Ghi chú mapping
- `note` và `deliveryRequirement` có trong spec nghiệp vụ
- nhưng **database hiện tại chưa có cột tương ứng trong `quotations`**
- nếu cần lưu lâu dài, nên bổ sung schema:
  - `quotations.note`
  - `quotations.delivery_requirement`

---

## 15. Mapping response model đề xuất

```json
{
  "id": "quotation-uuid",
  "quotationNumber": "QT-20260314-0001",
  "customerId": "customer-uuid",
  "projectId": "project-uuid",
  "status": "PENDING",
  "validUntil": "2026-03-29",
  "totalAmount": 26500000,
  "items": [
    {
      "id": "quotation-item-uuid-1",
      "productId": "product-uuid-1",
      "productCode": "HB100",
      "productName": "Steel H Beam",
      "type": "H-BEAM",
      "size": "100x100",
      "thickness": "6",
      "unit": "TON",
      "quantity": 10,
      "unitPrice": 1500000,
      "totalPrice": 15000000
    },
    {
      "id": "quotation-item-uuid-2",
      "productId": "product-uuid-2",
      "productCode": "PL200",
      "productName": "Steel Plate",
      "type": "PLATE",
      "size": "200x200",
      "thickness": "8",
      "unit": "TON",
      "quantity": 5,
      "unitPrice": 2300000,
      "totalPrice": 11500000
    }
  ],
  "createdAt": "2026-03-14T10:15:00+07:00"
}
```

---

## 16. Validation chi tiết

### 16.1 Validation authentication / authorization
- User phải đăng nhập
- Role phải là `CUSTOMER`
- Account status không được là `INACTIVE` hoặc `LOCKED`

### 16.2 Validation customer context
- Từ user hiện tại phải tìm được `customers.user_id`
- Customer status phải là `ACTIVE`

### 16.3 Validation project
- Nếu có `projectId`, project phải tồn tại
- Project phải thuộc `customer_id` hiện tại
- Project không được ở trạng thái đóng / khóa nếu policy không cho phép

### 16.4 Validation item list
- `items` bắt buộc có ít nhất 1 dòng
- `items.length <= 20`
- Mỗi `productId` là bắt buộc
- Không được duplicate `productId`
- `quantity > 0`

### 16.5 Validation product
- Product phải tồn tại
- Product phải có `status = ACTIVE`

### 16.6 Validation pricing
- Tìm được đúng bảng giá theo customer group
- Price list phải còn hiệu lực
- Price list phải có status hợp lệ
- Mỗi product phải có giá tương ứng

### 16.7 Validation total amount
- `total_amount >= 10,000,000`

---

## 17. Trạng thái và lifecycle

### 17.1 Trạng thái có thể dùng cho quotation
Theo spec và suy luận an toàn từ schema:
- `DRAFT`
- `PENDING`
- `APPROVED`
- `REJECTED`

### 17.2 Trạng thái khởi tạo
- Khi submit thành công: `PENDING`
- Khi lưu nháp: `DRAFT`

### 17.3 Hướng lifecycle đề xuất
```text
DRAFT -> PENDING -> APPROVED
                 -> REJECTED
```

---

## 18. Yêu cầu transaction

Luồng tạo quotation phải chạy trong **một database transaction**.

Trình tự đề xuất:
1. Validate toàn bộ dữ liệu đầu vào
2. Insert `quotations`
3. Insert tất cả `quotation_items`
4. Insert `audit_logs`
5. Commit

Nếu bất kỳ bước nào lỗi:
- rollback toàn bộ

---

## 19. Yêu cầu audit log

Khi tạo quotation thành công, hệ thống phải ghi log tối thiểu:

- `action = CREATE_QUOTATION`
- `entity_type = QUOTATION`
- `entity_id = quotations.id`
- `user_id = user hiện tại`
- `new_value` chứa snapshot cơ bản của quotation header + items

Ví dụ:

```json
{
  "action": "CREATE_QUOTATION",
  "entityType": "QUOTATION",
  "entityId": "quotation-uuid",
  "newValue": {
    "quotationNumber": "QT-20260314-0001",
    "customerId": "customer-uuid",
    "status": "PENDING",
    "totalAmount": 26500000
  }
}
```

---

## 20. Yêu cầu API đề xuất

### 20.1 API preview quotation
#### Endpoint
`POST /api/quotations/preview`

#### Mục đích
- tính giá
- validate rule
- chưa lưu dữ liệu

#### Request
```json
{
  "projectId": "optional-project-uuid",
  "items": [
    {
      "productId": "uuid-product-1",
      "quantity": 10
    }
  ]
}
```

#### Response
- trả line price
- tổng tiền
- validUntil dự kiến
- danh sách lỗi nếu có

### 20.2 API create quotation
#### Endpoint
`POST /api/quotations`

#### Mục đích
- tạo quotation chính thức
- lưu DB

#### Request
```json
{
  "projectId": "optional-project-uuid",
  "note": "optional note",
  "deliveryRequirement": "optional delivery requirement",
  "items": [
    {
      "productId": "uuid-product-1",
      "quantity": 10
    }
  ]
}
```

#### Response
- trả quotation header
- trả items
- trả status
- trả validUntil

### 20.3 API save draft quotation
#### Endpoint
`POST /api/quotations/draft`

#### Mục đích
- lưu bản nháp quotation

---

## 21. Pseudo workflow xử lý backend

```text
1. Lấy user hiện tại từ security context
2. Kiểm tra role CUSTOMER
3. Map sang customer record
4. Validate project (nếu có)
5. Validate item list
6. Load all products
7. Load active price list theo customer group và ngày hiện tại
8. Map unit price cho từng product
9. Tính line total và tổng tiền
10. Validate min amount và max line item
11. Sinh quotation number
12. Insert quotations
13. Insert quotation_items
14. Ghi audit log
15. Trả response
```

---

## 22. Khoảng trống giữa spec và database hiện tại

Dưới đây là các điểm chênh cần lưu ý khi implement:

### 22.1 `quotations.project_id` chưa có foreign key
- Cột có tồn tại
- Nhưng schema chưa khai báo FK tới `projects.id`

### 22.2 Chưa có cột cho delivery requirement / note
Spec có nhắc:
- project reference
- delivery requirements

Database `quotations` hiện chưa có:
- `note`
- `delivery_requirement`

### 22.3 Chưa có liên kết promotion
Spec có alternative flow áp dụng promotion, nhưng DB chưa có:
- `promotion_id`
- `discount_amount`
- `final_total_amount`

### 22.4 Chưa có notification mechanism
Spec yêu cầu gửi thông báo cho Accountant, nhưng schema chưa có:
- `notifications`
- `assigned_accountant_id`
- hoặc queue/event table

### 22.5 `customer_group` vs `customer_type`
Spec dùng khái niệm customer group / classification.
DB hiện có:
- `customers.customer_type`
- `price_lists.customer_group`

Cần thống nhất mapping nghiệp vụ:
- hoặc dùng `customer_type` làm đầu vào match `customer_group`
- hoặc bổ sung master data / enum mapping rõ ràng

---

## 23. Đề xuất cải tiến schema nếu muốn bám spec sát hơn

### 23.1 Bổ sung cột cho quotations
```sql
ALTER TABLE quotations
ADD COLUMN note VARCHAR(1000) NULL,
ADD COLUMN delivery_requirement VARCHAR(1000) NULL;
```

### 23.2 Bổ sung FK project
```sql
ALTER TABLE quotations
ADD CONSTRAINT fk_quotation_project
FOREIGN KEY (project_id) REFERENCES projects(id);
```

### 23.3 Bổ sung support promotion
```sql
ALTER TABLE quotations
ADD COLUMN promotion_id CHAR(36) NULL,
ADD COLUMN discount_amount DECIMAL(18,2) DEFAULT 0,
ADD COLUMN final_amount DECIMAL(18,2) DEFAULT 0;
```

### 23.4 Thêm unique constraint hợp lý
- `quotation_number` nên unique
- tránh duplicate mã quotation

Ví dụ:
```sql
ALTER TABLE quotations
ADD CONSTRAINT uk_quotation_number UNIQUE (quotation_number);
```

---

## 24. Tiêu chí chấp nhận nghiệp vụ

Một quotation được xem là tạo thành công khi:

1. Customer hợp lệ và đang active
2. Có ít nhất 1 item
3. Không vượt quá 20 item
4. Tất cả products đều active
5. Tất cả items đều có giá hợp lệ
6. Tổng tiền >= 10,000,000 VND
7. Header và items được lưu đầy đủ
8. Status được đặt đúng
9. Valid until được tính đúng +15 ngày
10. Có audit log

---

## 25. Test scenarios cốt lõi

### TC01 – Tạo quotation thành công
- Customer active
- 2 products active
- Có giá hợp lệ
- Tổng tiền > 10 triệu
- Kỳ vọng: tạo thành công, status = PENDING

### TC02 – Không có price list hợp lệ
- Không có bảng giá cho customer group
- Kỳ vọng: báo lỗi, không lưu DB

### TC03 – Product inactive
- 1 item chọn product inactive
- Kỳ vọng: chặn submit

### TC04 – Tổng tiền dưới ngưỡng
- Tổng tiền < 10 triệu
- Kỳ vọng: chặn submit

### TC05 – Vượt quá 20 dòng
- 21 products
- Kỳ vọng: chặn submit

### TC06 – Project không thuộc customer
- Gửi projectId của customer khác
- Kỳ vọng: từ chối

### TC07 – Duplicate product trong item list
- 2 dòng cùng productId
- Kỳ vọng: từ chối hoặc auto-merge theo rule đã chọn

### TC08 – Lỗi insert item
- Header insert thành công, item insert lỗi
- Kỳ vọng: rollback toàn bộ

---

## 26. Kết luận

Luồng **Tạo quotation** trong G90 là một luồng thương mại quan trọng, kết nối trực tiếp giữa:

- Customer self-service
- Pricing policy
- Project context
- Contract pipeline

Schema hiện tại đã đủ để triển khai phiên bản cơ bản của luồng này với:
- `quotations`
- `quotation_items`
- `price_lists`
- `price_list_items`

Tuy nhiên, để bám spec sát hơn, nên bổ sung:
- lưu delivery requirement / note
- FK project rõ ràng
- support promotion
- notification cho Accountant
- quy ước mapping customer group nhất quán
