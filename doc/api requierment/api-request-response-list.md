# API Request/Response List — Product Management (UI-30 → UI-34)

## 1. Scope

Tài liệu này chuẩn hóa lại bộ request/response cho các màn hình Product Management dựa trên:

- Canonical project spec G90
- Database schema trong `01_init.sql`

Phạm vi màn hình:

- UI-30 Product List
- UI-31 Create Product
- UI-32 Product Detail
- UI-33 Update Product
- UI-34 Delete Product

## 2. Database-aligned notes

### 2.1 Bảng nguồn chính
API trong tài liệu này bám trực tiếp vào bảng `products`:

- `id`
- `product_code`
- `product_name`
- `type`
- `size`
- `thickness`
- `unit`
- `weight_conversion`
- `reference_weight`
- `status`
- `created_at`

### 2.2 Các field KHÔNG có trong database hiện tại
Database hiện tại **không có** các field sau trong bảng `products`, nên **không đưa vào request/response chuẩn**:

- `description`
- `images`
- `updated_at`
- `updated_by`
- `deleted_at`
- `deleted_by`

### 2.3 Về nghiệp vụ Delete Product
Spec nghiệp vụ nói màn hình UI-34 là `Warehouse / Owner approval`, nhưng database hiện tại **không có bảng workflow/phê duyệt xóa sản phẩm** như:

- `product_delete_requests`
- `approval_requests`
- `product_status_history`

Vì vậy, bộ API chuẩn hóa theo database hiện tại sẽ dùng hướng:

- **soft delete bằng `status`**
- Owner approval là **business rule ở tầng ứng dụng** hoặc cần bổ sung schema sau

Tài liệu này vì vậy chuẩn hóa UI-34 theo endpoint cập nhật `status` của product sang `INACTIVE`.

---

## 3. Common response envelope

### 3.1 Success
```json
{
  "code": "SUCCESS",
  "message": "Success message",
  "data": {}
}
```

### 3.2 Validation error
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "productCode",
      "message": "Product code is required"
    }
  ]
}
```

### 3.3 Not found
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

### 3.4 Forbidden
```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to perform this action"
}
```

### 3.5 System error
```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "An unexpected error occurred"
}
```

---

## 4. Field mapping between API and database

| API field | DB column | Type |
|---|---|---|
| id | products.id | CHAR(36) |
| productCode | products.product_code | VARCHAR(50) |
| productName | products.product_name | VARCHAR(255) |
| type | products.type | VARCHAR(100) |
| size | products.size | VARCHAR(100) |
| thickness | products.thickness | VARCHAR(50) |
| unit | products.unit | VARCHAR(20) |
| weightConversion | products.weight_conversion | DECIMAL(10,4) |
| referenceWeight | products.reference_weight | DECIMAL(10,4) |
| status | products.status | VARCHAR(20) |
| createdAt | products.created_at | TIMESTAMP |

---

# 5. UI-30 — Product List

## 5.1 Purpose
Quản lý danh sách sản phẩm cho Warehouse:

- xem danh sách
- tìm kiếm
- lọc theo thuộc tính
- phân trang
- điều hướng sang detail / update / delete

## 5.2 Endpoint
**GET** `/api/products`

## 5.3 Query params

| Field | Type | Required | Description |
|---|---|---:|---|
| page | integer | No | Trang hiện tại, mặc định = 1 |
| pageSize | integer | No | Số bản ghi mỗi trang, mặc định = 20 |
| keyword | string | No | Tìm theo `productCode` hoặc `productName` |
| type | string | No | Lọc theo loại sản phẩm |
| size | string | No | Lọc theo kích thước |
| thickness | string | No | Lọc theo độ dày |
| unit | string | No | Lọc theo đơn vị |
| status | string | No | Lọc theo trạng thái, ví dụ `ACTIVE`, `INACTIVE` |
| sortBy | string | No | Trường sắp xếp, đề xuất: `createdAt`, `productCode`, `productName` |
| sortDir | string | No | `asc` hoặc `desc` |

## 5.4 Sample request
```http
GET /api/products?page=1&pageSize=20&keyword=ton&type=TON&thickness=1.5&status=ACTIVE&sortBy=createdAt&sortDir=desc
```

## 5.5 Success response
```json
{
  "code": "SUCCESS",
  "message": "Product list fetched successfully",
  "data": {
    "items": [
      {
        "id": "8d31e5f5-26c4-4a5c-8f8a-2d93cc712001",
        "productCode": "SP000523",
        "productName": "Tôn mạ kẽm G90 1.5mm",
        "type": "TON",
        "size": "200x6000",
        "thickness": "1.5",
        "unit": "TAM",
        "weightConversion": 4.2000,
        "referenceWeight": 4.0000,
        "status": "ACTIVE",
        "createdAt": "2026-03-13T10:30:00+07:00"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 125,
      "totalPages": 7
    },
    "filters": {
      "keyword": "ton",
      "type": "TON",
      "size": null,
      "thickness": "1.5",
      "unit": null,
      "status": "ACTIVE"
    }
  }
}
```

## 5.6 Error responses

### Invalid pagination
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "page",
      "message": "page must be greater than 0"
    }
  ]
}
```

### Load failed
```json
{
  "code": "PRODUCT_LIST_LOAD_FAILED",
  "message": "Unable to load product list"
}
```

---

# 6. UI-31 — Create Product

## 6.1 Purpose
Warehouse thêm mới sản phẩm.

## 6.2 Endpoint
**POST** `/api/products`

## 6.3 Request body

| Field | Type | Required | Description |
|---|---|---:|---|
| productCode | string | Yes | Mã sản phẩm, map `product_code`, unique |
| productName | string | Yes | Tên sản phẩm |
| type | string | Yes | Loại sản phẩm |
| size | string | Yes | Kích thước |
| thickness | string | Yes | Độ dày |
| unit | string | Yes | Đơn vị |
| weightConversion | number | No | Hệ số quy đổi trọng lượng |
| referenceWeight | number | No | Trọng lượng tham chiếu |
| status | string | No | Nếu không truyền, backend nên default `ACTIVE` |

## 6.4 Sample request
```json
{
  "productCode": "SP000524",
  "productName": "Thép tấm SS400 3.0mm",
  "type": "THEP_TAM",
  "size": "1500x6000",
  "thickness": "3.0",
  "unit": "TAM",
  "weightConversion": 141.3000,
  "referenceWeight": 141.3000,
  "status": "ACTIVE"
}
```

## 6.5 Success response
```json
{
  "code": "SUCCESS",
  "message": "Product created successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000524",
    "productName": "Thép tấm SS400 3.0mm",
    "type": "THEP_TAM",
    "size": "1500x6000",
    "thickness": "3.0",
    "unit": "TAM",
    "weightConversion": 141.3000,
    "referenceWeight": 141.3000,
    "status": "ACTIVE",
    "createdAt": "2026-03-13T11:00:00+07:00"
  }
}
```

## 6.6 Error responses

### Duplicate product code
```json
{
  "code": "PRODUCT_CODE_ALREADY_EXISTS",
  "message": "Product code already exists",
  "errors": [
    {
      "field": "productCode",
      "message": "Product code must be unique"
    }
  ]
}
```

### Required fields missing
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "productName",
      "message": "Product name is required"
    },
    {
      "field": "unit",
      "message": "Unit is required"
    }
  ]
}
```

---

# 7. UI-32 — Product Detail

## 7.1 Purpose
Xem thông tin chi tiết sản phẩm.

## 7.2 Endpoint
**GET** `/api/products/{id}`

## 7.3 Path params

| Field | Type | Required | Description |
|---|---|---:|---|
| id | string | Yes | Product ID, map `products.id` |

## 7.4 Sample request
```http
GET /api/products/0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002
```

## 7.5 Success response
```json
{
  "code": "SUCCESS",
  "message": "Product detail fetched successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000524",
    "productName": "Thép tấm SS400 3.0mm",
    "type": "THEP_TAM",
    "size": "1500x6000",
    "thickness": "3.0",
    "unit": "TAM",
    "weightConversion": 141.3000,
    "referenceWeight": 141.3000,
    "status": "ACTIVE",
    "createdAt": "2026-03-13T11:00:00+07:00"
  }
}
```

## 7.6 Error responses

### Product not found
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

---

# 8. UI-33 — Update Product

## 8.1 Purpose
Warehouse cập nhật thông tin sản phẩm.

## 8.2 Endpoint
**PUT** `/api/products/{id}`

## 8.3 Path params

| Field | Type | Required | Description |
|---|---|---:|---|
| id | string | Yes | Product ID |

## 8.4 Request body

| Field | Type | Required | Description |
|---|---|---:|---|
| productCode | string | Yes | Mã sản phẩm |
| productName | string | Yes | Tên sản phẩm |
| type | string | Yes | Loại sản phẩm |
| size | string | Yes | Kích thước |
| thickness | string | Yes | Độ dày |
| unit | string | Yes | Đơn vị |
| weightConversion | number | No | Hệ số quy đổi |
| referenceWeight | number | No | Trọng lượng tham chiếu |
| status | string | Yes | Trạng thái hiện tại |

## 8.5 Sample request
```json
{
  "productCode": "SP000524",
  "productName": "Thép tấm SS400 3.2mm",
  "type": "THEP_TAM",
  "size": "1500x6000",
  "thickness": "3.2",
  "unit": "TAM",
  "weightConversion": 150.8000,
  "referenceWeight": 150.8000,
  "status": "ACTIVE"
}
```

## 8.6 Success response
```json
{
  "code": "SUCCESS",
  "message": "Product updated successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000524",
    "productName": "Thép tấm SS400 3.2mm",
    "type": "THEP_TAM",
    "size": "1500x6000",
    "thickness": "3.2",
    "unit": "TAM",
    "weightConversion": 150.8000,
    "referenceWeight": 150.8000,
    "status": "ACTIVE",
    "createdAt": "2026-03-13T11:00:00+07:00"
  }
}
```

## 8.7 Error responses

### Product not found
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

### Duplicate product code
```json
{
  "code": "PRODUCT_CODE_ALREADY_EXISTS",
  "message": "Product code already exists",
  "errors": [
    {
      "field": "productCode",
      "message": "Product code must be unique"
    }
  ]
}
```

---

# 9. UI-34 — Delete Product

## 9.1 Purpose
Xóa sản phẩm khỏi danh sách sử dụng.

## 9.2 Database-aligned interpretation
Do database hiện tại không có workflow approval riêng, API chuẩn hóa theo DB sẽ dùng:

- **soft delete**
- set `products.status = 'INACTIVE'`

Nghĩa là UI vẫn gọi là Delete Product, nhưng ở mức dữ liệu thực tế sẽ là **deactivate product**.

## 9.3 Endpoint
**PATCH** `/api/products/{id}/status`

## 9.4 Path params

| Field | Type | Required | Description |
|---|---|---:|---|
| id | string | Yes | Product ID |

## 9.5 Request body

| Field | Type | Required | Description |
|---|---|---:|---|
| status | string | Yes | Giá trị đề xuất dùng cho UI-34 là `INACTIVE` |
| reason | string | No | Lý do ngừng sử dụng sản phẩm |
| requestedByRole | string | No | Phục vụ kiểm tra business rule ở application layer |

## 9.6 Sample request
```json
{
  "status": "INACTIVE",
  "reason": "Product discontinued",
  "requestedByRole": "WAREHOUSE"
}
```

## 9.7 Success response
```json
{
  "code": "SUCCESS",
  "message": "Product status updated successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000524",
    "productName": "Thép tấm SS400 3.2mm",
    "status": "INACTIVE",
    "createdAt": "2026-03-13T11:00:00+07:00"
  }
}
```

## 9.8 Error responses

### Product not found
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

### Invalid status
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "status",
      "message": "status must be ACTIVE or INACTIVE"
    }
  ]
}
```

### Approval rule not satisfied
```json
{
  "code": "OWNER_APPROVAL_REQUIRED",
  "message": "Owner approval is required to delete product"
}
```

> Ghi chú: response `OWNER_APPROVAL_REQUIRED` là hợp lệ theo business spec, nhưng để implement đúng hoàn toàn thì database cần bổ sung bảng approval/workflow riêng.

---

# 10. Suggested endpoint summary

| Screen ID | Screen Name | Method | Endpoint |
|---|---|---|---|
| UI-30 | Product List | GET | `/api/products` |
| UI-31 | Create Product | POST | `/api/products` |
| UI-32 | Product Detail | GET | `/api/products/{id}` |
| UI-33 | Update Product | PUT | `/api/products/{id}` |
| UI-34 | Delete Product | PATCH | `/api/products/{id}/status` |

---

# 11. Recommended validation rules

## 11.1 Request validation

| Field | Rule |
|---|---|
| productCode | required, max 50, unique |
| productName | required, max 255 |
| type | required, max 100 |
| size | required, max 100 |
| thickness | required, max 50 |
| unit | required, max 20 |
| weightConversion | optional, numeric, >= 0 |
| referenceWeight | optional, numeric, >= 0 |
| status | optional on create, required on update/status change |

## 11.2 Suggested allowed status values

| Value | Meaning |
|---|---|
| ACTIVE | Sản phẩm đang sử dụng |
| INACTIVE | Sản phẩm ngừng sử dụng / soft delete |

---

# 12. Important implementation note

Nếu team muốn bám đúng cả spec lẫn DB theo nghiệp vụ `Warehouse / Owner approval` cho UI-34, nên bổ sung thêm ít nhất một trong các phương án schema sau:

## Option A — Dedicated approval table
- `product_delete_requests`

## Option B — Generic approval table
- `approval_requests`
- `approval_request_actions`

## Option C — Audit only, application-managed workflow
- tận dụng `audit_logs`, nhưng vẫn cần lưu trạng thái chờ duyệt ở nơi khác

Ở trạng thái database hiện tại, tài liệu này đã chuẩn hóa request/response theo cách **khả thi nhất và bám schema nhất**.
