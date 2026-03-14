# API Request/Response - Module Pricing

## 1. Overview

Tai lieu nay duoc tao tu module Pricing da implement trong backend.

Base path:

```http
/api/price-lists
```

Response envelope thanh cong:

```json
{
  "code": "SUCCESS",
  "message": "Success message",
  "data": {}
}
```

Response envelope loi:

```json
{
  "code": "ERROR_CODE",
  "message": "Error message",
  "errors": [
    {
      "field": "fieldName",
      "message": "Validation message"
    }
  ]
}
```

## 2. API List

| No | Method | Endpoint | Description |
|---|---|---|---|
| 1 | POST | `/api/price-lists` | Tao moi bang gia |
| 2 | GET | `/api/price-lists` | Lay danh sach bang gia |
| 3 | GET | `/api/price-lists/{id}` | Lay chi tiet bang gia kem danh sach gia san pham |
| 4 | PUT | `/api/price-lists/{id}` | Cap nhat thong tin bang gia |
| 5 | DELETE | `/api/price-lists/{id}` | Xoa bang gia |
| 6 | POST | `/api/price-lists/{id}/items` | Them san pham vao bang gia |
| 7 | PUT | `/api/price-list-items/{id}` | Cap nhat don gia cua item |
| 8 | DELETE | `/api/price-list-items/{id}` | Xoa item khoi bang gia |

---

## 3. POST /api/price-lists

### 3.1 Request Body

```json
{
  "name": "Bang gia 2026",
  "customerGroup": "CONTRACTOR",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE"
}
```

### 3.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| name | string | Yes | not blank, max 255 |
| customerGroup | string | No | max 50 |
| startDate | date | Yes | phai nho hon `endDate` |
| endDate | date | Yes | phai lon hon `startDate` |
| status | string | No | `ACTIVE` hoac `INACTIVE`, mac dinh la `ACTIVE`, max 20 |

### 3.3 Success Response

HTTP status:

```http
201 Created
```

```json
{
  "code": "SUCCESS",
  "message": "Price list created successfully",
  "data": {
    "id": "8b8eea26-0f19-4d42-89f8-5d6d484fd001"
  }
}
```

### 3.4 Error Responses

Date range khong hop le:

```json
{
  "code": "MSG50",
  "message": "Invalid date range",
  "errors": [
    {
      "field": "startDate",
      "message": "Start date must be before end date"
    }
  ]
}
```

Status khong hop le:

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

---

## 4. GET /api/price-lists

### 4.1 Query Parameters

| Field | Type | Required | Default | Rules |
|---|---|---:|---|---|
| page | integer | No | `0` | `>= 0` |
| size | integer | No | `10` | `> 0` |
| status | string | No | | `ACTIVE` hoac `INACTIVE` |
| customerGroup | string | No | | exact match, max 50 |

### 4.2 Sample Request

```http
GET /api/price-lists?page=0&size=10&status=ACTIVE&customerGroup=CONTRACTOR
```

### 4.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Price list fetched successfully",
  "data": {
    "content": [
      {
        "id": "8b8eea26-0f19-4d42-89f8-5d6d484fd001",
        "name": "Bang gia 2026",
        "customerGroup": "CONTRACTOR",
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "status": "ACTIVE",
        "createdAt": "2026-03-14T15:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1
  }
}
```

### 4.4 Error Responses

Invalid page:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "page",
      "message": "page must be greater than or equal to 0"
    }
  ]
}
```

Invalid status:

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

---

## 5. GET /api/price-lists/{id}

### 5.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | Price list ID |

### 5.2 Sample Request

```http
GET /api/price-lists/8b8eea26-0f19-4d42-89f8-5d6d484fd001
```

### 5.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Price list detail fetched successfully",
  "data": {
    "id": "8b8eea26-0f19-4d42-89f8-5d6d484fd001",
    "name": "Bang gia 2026",
    "customerGroup": "CONTRACTOR",
    "startDate": "2026-01-01",
    "endDate": "2026-12-31",
    "status": "ACTIVE",
    "items": [
      {
        "id": "6b02ac48-b608-4ad1-b423-a5fce8b4d010",
        "productId": "3c08d8f0-6a1d-4c54-9b40-000000000001",
        "productName": "Ton ma kem G90 0.45mm",
        "unitPrice": 15000000.00
      }
    ]
  }
}
```

### 5.4 Error Response

```json
{
  "code": "PRICE_LIST_NOT_FOUND",
  "message": "Price list not found"
}
```

---

## 6. PUT /api/price-lists/{id}

### 6.1 Request Body

```json
{
  "name": "Bang gia 2026 dieu chinh",
  "customerGroup": "DISTRIBUTOR",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "INACTIVE"
}
```

### 6.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| name | string | Yes | not blank, max 255 |
| customerGroup | string | No | max 50 |
| startDate | date | Yes | phai nho hon `endDate` |
| endDate | date | Yes | phai lon hon `startDate` |
| status | string | Yes | `ACTIVE` hoac `INACTIVE` |

### 6.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Price list updated successfully"
}
```

### 6.4 Error Responses

Price list khong ton tai:

```json
{
  "code": "PRICE_LIST_NOT_FOUND",
  "message": "Price list not found"
}
```

Date range khong hop le:

```json
{
  "code": "MSG50",
  "message": "Invalid date range",
  "errors": [
    {
      "field": "startDate",
      "message": "Start date must be before end date"
    }
  ]
}
```

---

## 7. DELETE /api/price-lists/{id}

### 7.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | Price list ID |

### 7.2 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Price list deleted successfully"
}
```

### 7.3 Error Response

```json
{
  "code": "PRICE_LIST_NOT_FOUND",
  "message": "Price list not found"
}
```

---

## 8. POST /api/price-lists/{id}/items

### 8.1 Request Body

```json
{
  "productId": "3c08d8f0-6a1d-4c54-9b40-000000000001",
  "unitPrice": 15000000
}
```

### 8.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| productId | string | Yes | not blank, max 36, phai ton tai trong `products` |
| unitPrice | number | Yes | `> 0` |

### 8.3 Success Response

HTTP status:

```http
201 Created
```

```json
{
  "code": "SUCCESS",
  "message": "Price item added successfully",
  "data": {
    "id": "6b02ac48-b608-4ad1-b423-a5fce8b4d010"
  }
}
```

### 8.4 Error Responses

Price list khong ton tai:

```json
{
  "code": "PRICE_LIST_NOT_FOUND",
  "message": "Price list not found"
}
```

Product khong ton tai:

```json
{
  "code": "MSG25",
  "message": "Product not found",
  "errors": [
    {
      "field": "productId",
      "message": "Product not found"
    }
  ]
}
```

Don gia khong hop le:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "unitPrice",
      "message": "Unit price must be greater than 0"
    }
  ]
}
```

---

## 9. PUT /api/price-list-items/{id}

### 9.1 Request Body

```json
{
  "unitPrice": 15500000
}
```

### 9.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| unitPrice | number | Yes | `> 0` |

### 9.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Price item updated successfully"
}
```

### 9.4 Error Responses

Item khong ton tai:

```json
{
  "code": "PRICE_LIST_ITEM_NOT_FOUND",
  "message": "Price list item not found"
}
```

---

## 10. DELETE /api/price-list-items/{id}

### 10.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | Price list item ID |

### 10.2 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Price item deleted successfully"
}
```

### 10.3 Error Response

```json
{
  "code": "PRICE_LIST_ITEM_NOT_FOUND",
  "message": "Price list item not found"
}
```

---

## 11. Implemented Response DTO Summary

### 11.1 PriceListCreateDataResponse

```json
{
  "id": "string"
}
```

### 11.2 PriceListListItemResponse

```json
{
  "id": "string",
  "name": "string",
  "customerGroup": "string",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE",
  "createdAt": "2026-03-14T15:00:00"
}
```

### 11.3 PriceListListResponseData

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0
}
```

### 11.4 PriceListDetailResponse

```json
{
  "id": "string",
  "name": "string",
  "customerGroup": "string",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE",
  "items": [
    {
      "id": "string",
      "productId": "string",
      "productName": "string",
      "unitPrice": 0.00
    }
  ]
}
```

### 11.5 PriceListItemCreateDataResponse

```json
{
  "id": "string"
}
```

## 12. Implementation Notes

- Module Pricing da duoc code theo pattern `ApiResponse` chung cua backend.
- `POST /api/price-lists` va `POST /api/price-lists/{id}/items` tra ve HTTP `201 Created`.
- `GET`, `PUT`, `DELETE` tra ve HTTP `200 OK` khi thanh cong.
- Cac action create, update, delete deu ghi vao `audit_logs`.
- Hien tai `audit_logs.user_id` va `price_lists.created_by` dang de `null` vi project chua co DB-backed JWT authorization.
- Rule "chi xoa bang gia neu khong duoc dung boi hop dong active" chua enforce o runtime, vi schema hien tai khong co lien ket truc tiep tu `contracts` sang `price_lists`.
- Security thuc te hien tai van la Basic Auth toi thieu cua project, chua la JWT nhu spec.
