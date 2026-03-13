# API Request/Response - Module Product

## 1. Overview

Tai lieu nay duoc tao tu module Product da implement trong backend.

Base path:

```http
/api/products
```

Response envelope thanh cong:

```json
{
  "code": "SUCCESS",
  "message": "Success message",
  "data": {}
}
```

Response envelope loi validation:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "fieldName",
      "message": "Validation message"
    }
  ]
}
```

Response envelope loi chung:

```json
{
  "code": "ERROR_CODE",
  "message": "Error message"
}
```

## 2. API List

| No | Method | Endpoint | Description |
|---|---|---|---|
| 1 | GET | `/api/products` | Lay danh sach san pham co phan trang va bo loc |
| 2 | POST | `/api/products` | Tao moi san pham |
| 3 | GET | `/api/products/{id}` | Lay chi tiet san pham |
| 4 | PUT | `/api/products/{id}` | Cap nhat san pham |
| 5 | PATCH | `/api/products/{id}/status` | Cap nhat trang thai san pham, dung cho soft delete |

---

## 3. GET /api/products

### 3.1 Query Parameters

| Field | Type | Required | Default | Notes |
|---|---|---:|---|---|
| page | integer | No | `1` | `>= 1` |
| pageSize | integer | No | `20` | `>= 1` |
| keyword | string | No | | Tim theo `productCode` hoac `productName` |
| type | string | No | | Loc theo loai san pham |
| size | string | No | | Loc theo kich thuoc |
| thickness | string | No | | Loc theo do day |
| unit | string | No | | Loc theo don vi |
| status | string | No | | `ACTIVE` hoac `INACTIVE` |
| sortBy | string | No | `createdAt` | `createdAt`, `productCode`, `productName` |
| sortDir | string | No | `desc` | `asc` hoac `desc` |

### 3.2 Sample Request

```http
GET /api/products?page=1&pageSize=20&keyword=ton&type=TON&thickness=1.5&status=ACTIVE&sortBy=createdAt&sortDir=desc
```

### 3.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Product list fetched successfully",
  "data": {
    "items": [
      {
        "id": "3c08d8f0-6a1d-4c54-9b40-000000000001",
        "productCode": "SP000001",
        "productName": "Ton ma kem G90 0.45mm",
        "type": "TON",
        "size": "1200x6000",
        "thickness": "0.45",
        "unit": "TAM",
        "weightConversion": 4.2000,
        "referenceWeight": 4.0000,
        "status": "ACTIVE",
        "createdAt": "2026-03-10T08:00:00+07:00"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 10,
      "totalPages": 1
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

### 3.4 Error Responses

Invalid pagination:

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

Invalid sort:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "sortBy",
      "message": "sortBy must be one of createdAt, productCode, productName"
    }
  ]
}
```

Load failed:

```json
{
  "code": "PRODUCT_LIST_LOAD_FAILED",
  "message": "Unable to load product list"
}
```

---

## 4. POST /api/products

### 4.1 Request Body

```json
{
  "productCode": "SP000011",
  "productName": "Thep tam SS400 8.0mm",
  "type": "THEP_TAM",
  "size": "1500x6000",
  "thickness": "8.0",
  "unit": "TAM",
  "weightConversion": 377.0000,
  "referenceWeight": 377.0000,
  "status": "ACTIVE"
}
```

### 4.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| productCode | string | Yes | not blank, max 50, unique |
| productName | string | Yes | not blank, max 255 |
| type | string | Yes | not blank, max 100 |
| size | string | Yes | not blank, max 100 |
| thickness | string | Yes | not blank, max 50 |
| unit | string | Yes | not blank, max 20 |
| weightConversion | number | No | `>= 0` |
| referenceWeight | number | No | `>= 0` |
| status | string | No | `ACTIVE` hoac `INACTIVE`, mac dinh backend la `ACTIVE` |

### 4.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Product created successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000011",
    "productName": "Thep tam SS400 8.0mm",
    "type": "THEP_TAM",
    "size": "1500x6000",
    "thickness": "8.0",
    "unit": "TAM",
    "weightConversion": 377.0000,
    "referenceWeight": 377.0000,
    "status": "ACTIVE",
    "createdAt": "2026-03-13T15:00:00+07:00"
  }
}
```

### 4.4 Error Responses

Duplicate product code:

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

Missing required field:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "productName",
      "message": "Product name is required"
    }
  ]
}
```

---

## 5. GET /api/products/{id}

### 5.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | Product ID |

### 5.2 Sample Request

```http
GET /api/products/3c08d8f0-6a1d-4c54-9b40-000000000001
```

### 5.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Product detail fetched successfully",
  "data": {
    "id": "3c08d8f0-6a1d-4c54-9b40-000000000001",
    "productCode": "SP000001",
    "productName": "Ton ma kem G90 0.45mm",
    "type": "TON",
    "size": "1200x6000",
    "thickness": "0.45",
    "unit": "TAM",
    "weightConversion": 4.2000,
    "referenceWeight": 4.0000,
    "status": "ACTIVE",
    "createdAt": "2026-03-10T08:00:00+07:00"
  }
}
```

### 5.4 Error Response

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

---

## 6. PUT /api/products/{id}

### 6.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | Product ID |

### 6.2 Request Body

```json
{
  "productCode": "SP000011",
  "productName": "Thep tam SS400 8.2mm",
  "type": "THEP_TAM",
  "size": "1500x6000",
  "thickness": "8.2",
  "unit": "TAM",
  "weightConversion": 386.4000,
  "referenceWeight": 386.4000,
  "status": "ACTIVE"
}
```

### 6.3 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| productCode | string | Yes | not blank, max 50, unique |
| productName | string | Yes | not blank, max 255 |
| type | string | Yes | not blank, max 100 |
| size | string | Yes | not blank, max 100 |
| thickness | string | Yes | not blank, max 50 |
| unit | string | Yes | not blank, max 20 |
| weightConversion | number | No | `>= 0` |
| referenceWeight | number | No | `>= 0` |
| status | string | Yes | `ACTIVE` hoac `INACTIVE` |

### 6.4 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Product updated successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000011",
    "productName": "Thep tam SS400 8.2mm",
    "type": "THEP_TAM",
    "size": "1500x6000",
    "thickness": "8.2",
    "unit": "TAM",
    "weightConversion": 386.4000,
    "referenceWeight": 386.4000,
    "status": "ACTIVE",
    "createdAt": "2026-03-13T15:00:00+07:00"
  }
}
```

### 6.5 Error Responses

Product not found:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

Duplicate product code:

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

## 7. PATCH /api/products/{id}/status

### 7.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | Product ID |

### 7.2 Request Body

```json
{
  "status": "INACTIVE",
  "reason": "Product discontinued",
  "requestedByRole": "WAREHOUSE"
}
```

### 7.3 Notes

- API nay duoc dung cho cap nhat trang thai san pham.
- Soft delete duoc thuc hien bang cach set `status = INACTIVE`.
- `reason` va `requestedByRole` hien duoc nhan vao request nhung chua luu vao database.
- Truong `status` dang duoc validate tai service layer.

### 7.4 Success Response

```json
{
  "code": "SUCCESS",
  "message": "Product status updated successfully",
  "data": {
    "id": "0e6a3e7d-1ea7-4fe7-bf90-2fbf6d0bb002",
    "productCode": "SP000011",
    "productName": "Thep tam SS400 8.2mm",
    "status": "INACTIVE",
    "createdAt": "2026-03-13T15:00:00+07:00"
  }
}
```

### 7.5 Error Responses

Product not found:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
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

## 8. Implemented Response DTO Summary

### 8.1 ProductResponse

```json
{
  "id": "string",
  "productCode": "string",
  "productName": "string",
  "type": "string",
  "size": "string",
  "thickness": "string",
  "unit": "string",
  "weightConversion": 0.0000,
  "referenceWeight": 0.0000,
  "status": "ACTIVE",
  "createdAt": "2026-03-13T15:00:00+07:00"
}
```

### 8.2 ProductStatusResponse

```json
{
  "id": "string",
  "productCode": "string",
  "productName": "string",
  "status": "ACTIVE",
  "createdAt": "2026-03-13T15:00:00+07:00"
}
```

### 8.3 ProductListResponseData

```json
{
  "items": [],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 0,
    "totalPages": 0
  },
  "filters": {
    "keyword": null,
    "type": null,
    "size": null,
    "thickness": null,
    "unit": null,
    "status": null
  }
}
```

## 9. Implementation Notes

- Tai lieu nay bam theo module da code trong backend, khong phai theo mock spec.
- `POST /api/products` tra ve HTTP status `201 Created`.
- `GET`, `PUT`, `PATCH` tra ve HTTP status `200 OK` khi thanh cong.
- `createdAt` duoc response theo dinh dang `yyyy-MM-dd'T'HH:mm:ssXXX`.
- Cac endpoint Product dang duoc permit trong security config de test API.
