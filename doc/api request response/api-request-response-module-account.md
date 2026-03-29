# API Request/Response - Module Account

## 1. Overview

Tai lieu nay duoc tao tu module Account da implement trong backend.

Base path:

```http
/api/accounts
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

## 2. Seed Role Reference

Module Account dang map `roleId` theo du lieu seed:

| Role | roleId |
|---|---|
| OWNER | `11111111-1111-1111-1111-111111111111` |
| ACCOUNTANT | `22222222-2222-2222-2222-222222222222` |
| WAREHOUSE | `33333333-3333-3333-3333-333333333333` |
| CUSTOMER | `44444444-4444-4444-4444-444444444444` |

Luu y:

- `POST /api/accounts` chi chap nhan role `ACCOUNTANT` hoac `WAREHOUSE`
- `PUT /api/accounts/{id}` chi chap nhan role assignable la `ACCOUNTANT` hoac `WAREHOUSE`
- Neu account dich dang co role `OWNER`, API khong cho doi role

## 3. API List

| No | Method | Endpoint | Description |
|---|---|---|---|
| 1 | POST | `/api/accounts` | Tao moi tai khoan noi bo |
| 2 | GET | `/api/accounts` | Lay danh sach tai khoan noi bo |
| 3 | GET | `/api/accounts/{id}` | Lay chi tiet tai khoan |
| 4 | PUT | `/api/accounts/{id}` | Cap nhat tai khoan |
| 5 | PATCH | `/api/accounts/{id}/deactivate` | Deactivate tai khoan |

---

## 4. POST /api/accounts

### 4.1 Request Body

```json
{
  "fullName": "Nguyen Van A",
  "email": "accountant@g90steel.vn",
  "password": "password123",
  "phone": "0901234567",
  "address": "Ha Noi",
  "roleId": "22222222-2222-2222-2222-222222222222"
}
```

### 4.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| fullName | string | Yes | not blank |
| email | string | Yes | not blank, valid email, unique |
| password | string | Yes | not blank, min 6 chars, duoc luu bang bcrypt hash |
| phone | string | No | optional |
| address | string | No | optional |
| roleId | string | Yes | role phai hop le va chi duoc la `ACCOUNTANT` hoac `WAREHOUSE` |

### 4.3 Success Response

HTTP status:

```http
201 Created
```

```json
{
  "code": "SUCCESS",
  "message": "User account created successfully",
  "data": {
    "id": "a4cc6c2b-1f22-4d46-b43f-e1c0b4bb9001"
  }
}
```

### 4.4 Error Responses

Email da ton tai:

```json
{
  "code": "MSG21",
  "message": "Email already exists",
  "errors": [
    {
      "field": "email",
      "message": "Email must be unique"
    }
  ]
}
```

Role khong hop le:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "roleId",
      "message": "Role must be ACCOUNTANT or WAREHOUSE"
    }
  ]
}
```

Request body thieu field:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "fullName",
      "message": "Full name is required"
    }
  ]
}
```

---

## 5. GET /api/accounts

### 5.1 Query Parameters

| Field | Type | Required | Default | Rules |
|---|---|---:|---|---|
| page | integer | No | `0` | `>= 0` |
| size | integer | No | `10` | `> 0` |
| role | string | No | | role name filter |
| status | string | No | | `ACTIVE`, `INACTIVE`, `LOCKED` |

### 5.2 Notes

- Danh sach nay chi tra ve tai khoan noi bo.
- Account co role `CUSTOMER` bi loai ra khoi ket qua.
- Danh sach duoc sort theo `createdAt desc`.

### 5.3 Sample Request

```http
GET /api/accounts?page=1&pageSize=10&role=ACCOUNTANT&status=ACTIVE
```

### 5.4 Success Response

```json
{
  "code": "SUCCESS",
  "message": "User list fetched successfully",
  "data": {
    "content": [
      {
        "id": "a4cc6c2b-1f22-4d46-b43f-e1c0b4bb9001",
        "fullName": "Nguyen Van A",
        "email": "accountant@g90steel.vn",
        "role": "ACCOUNTANT",
        "status": "ACTIVE",
        "createdAt": "2026-03-13T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1
  }
}
```

### 5.5 Error Responses

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
      "message": "status must be ACTIVE, INACTIVE, or LOCKED"
    }
  ]
}
```

Invalid role:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "role",
      "message": "Role is invalid"
    }
  ]
}
```

---

## 6. GET /api/accounts/{id}

### 6.1 Path Parameter

| Field | Type | Required | Notes |
|---|---|---:|---|
| id | string | Yes | User account ID |

### 6.2 Sample Request

```http
GET /api/accounts/a4cc6c2b-1f22-4d46-b43f-e1c0b4bb9001
```

### 6.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "User detail fetched successfully",
  "data": {
    "id": "a4cc6c2b-1f22-4d46-b43f-e1c0b4bb9001",
    "fullName": "Nguyen Van A",
    "email": "accountant@g90steel.vn",
    "phone": "0901234567",
    "address": "Ha Noi",
    "role": "ACCOUNTANT",
    "status": "ACTIVE",
    "createdAt": "2026-03-13T10:00:00"
  }
}
```

### 6.4 Error Response

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "User account not found"
}
```

---

## 7. PUT /api/accounts/{id}

### 7.1 Request Body

```json
{
  "fullName": "Nguyen Van B",
  "phone": "0908888888",
  "address": "Ho Chi Minh",
  "roleId": "33333333-3333-3333-3333-333333333333",
  "status": "ACTIVE"
}
```

### 7.2 Field Rules

| Field | Type | Required | Rules |
|---|---|---:|---|
| fullName | string | Yes | not blank |
| phone | string | No | optional |
| address | string | No | optional |
| roleId | string | Yes | role hop le; account khong duoc doi sang role khac neu target account dang la `OWNER` |
| status | string | Yes | `ACTIVE`, `INACTIVE`, `LOCKED` |

### 7.3 Behavior

- Email khong duoc update vi request body hien tai khong cho phep sua `email`
- Neu target account la owner va roleId moi khac role hien tai, API tra `FORBIDDEN`

### 7.4 Success Response

```json
{
  "code": "SUCCESS",
  "message": "User account updated successfully"
}
```

### 7.5 Error Responses

Account khong ton tai:

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "User account not found"
}
```

Role khong hop le:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "roleId",
      "message": "Role must be ACCOUNTANT or WAREHOUSE"
    }
  ]
}
```

Owner role change bi chan:

```json
{
  "code": "FORBIDDEN",
  "message": "Owner cannot change own role"
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
      "message": "status must be ACTIVE, INACTIVE, or LOCKED"
    }
  ]
}
```

---

## 8. PATCH /api/accounts/{id}/deactivate

### 8.1 Request Body

```json
{
  "reason": "Employee resigned"
}
```

### 8.2 Behavior

- Set `status = INACTIVE`
- Khong xoa du lieu
- Tao audit log voi action `DEACTIVATE_USER`
- Neu target account la `OWNER`, API tra `FORBIDDEN`

### 8.3 Success Response

```json
{
  "code": "SUCCESS",
  "message": "User account deactivated successfully"
}
```

### 8.4 Error Responses

Account khong ton tai:

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "User account not found"
}
```

Forbidden:

```json
{
  "code": "FORBIDDEN",
  "message": "Permission denied"
}
```

---

## 9. Implemented Response DTO Summary

### 9.1 AccountCreateDataResponse

```json
{
  "id": "string"
}
```

### 9.2 AccountListItemResponse

```json
{
  "id": "string",
  "fullName": "string",
  "email": "string",
  "role": "ACCOUNTANT",
  "status": "ACTIVE",
  "createdAt": "2026-03-13T10:00:00"
}
```

### 9.3 AccountListResponseData

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0
}
```

### 9.4 AccountDetailResponse

```json
{
  "id": "string",
  "fullName": "string",
  "email": "string",
  "phone": "string",
  "address": "string",
  "role": "ACCOUNTANT",
  "status": "ACTIVE",
  "createdAt": "2026-03-13T10:00:00"
}
```

## 10. Implementation Notes

- Module Account da duoc code theo pattern `ApiResponse` chung cua backend.
- `POST /api/accounts` tra ve HTTP `201 Created`.
- `GET`, `PUT`, `PATCH` tra ve HTTP `200 OK` khi thanh cong.
- Cac action create/list/detail/update/deactivate deu ghi vao `audit_logs`.
- Password duoc hash bang `BCryptPasswordEncoder`.
- Hien tai project chua co JWT module hoan chinh; security request-level cua spec Owner/JWT chua duoc wire day du vao account domain, nhung business rule core cua module da duoc implement.
