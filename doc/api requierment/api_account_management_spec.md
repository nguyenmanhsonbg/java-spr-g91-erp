# API Requirement Specification

## Module: Account Management (Owner)

### Project

G90 Steel Business Management System

### Source References

-   Project Spec: G90 Steel Business Management System Spec
-   Database Schema: V1\_\_init.sql

This document defines API requirements for **Account Management** where
the **Owner** manages internal user accounts (Accountant, Warehouse).

------------------------------------------------------------------------

# 1. Module Overview

The Account Management module allows the **Owner** to manage internal
system accounts.

Main responsibilities:

-   Create internal user accounts
-   View user list and details
-   Update user accounts
-   Deactivate user accounts

Roles supported by this module:

  Role         Permission
  ------------ -------------
  Owner        Full access
  Accountant   No access
  Warehouse    No access
  Customer     No access

Database tables involved:

-   `users`
-   `roles`
-   `audit_logs`

------------------------------------------------------------------------

# 2. API List

  ------------------------------------------------------------------------------------
  API ID       API Name              Method            Endpoint
  ------------ --------------------- ----------------- -------------------------------
  ACC-01       Create User Account   POST              /api/accounts

  ACC-02       Get User List         GET               /api/accounts

  ACC-03       Get User Detail       GET               /api/accounts/{id}

  ACC-04       Update User Account   PUT               /api/accounts/{id}

  ACC-05       Deactivate User       PATCH             /api/accounts/{id}/deactivate
               Account                                 
  ------------------------------------------------------------------------------------

------------------------------------------------------------------------

# 3. API Specifications

------------------------------------------------------------------------

# ACC-01 Create User Account

Create a new internal user account.

### Endpoint

    POST /api/accounts

### Authorization

Owner

### Request Body

``` json
{
  "fullName": "Nguyen Van A",
  "email": "accountant@g90steel.vn",
  "password": "password123",
  "phone": "0901234567",
  "address": "Ha Noi",
  "roleId": "UUID_ROLE_ACCOUNTANT"
}
```

### Validation Rules

-   Email must be unique
-   Password minimum length = 6 characters
-   Role must be `ACCOUNTANT` or `WAREHOUSE`

### Success Response

``` json
{
  "id": "UUID",
  "message": "User account created successfully"
}
```

### Error Responses

  Code   Description
  ------ ----------------------
  400    Invalid input
  409    Email already exists
  403    Permission denied

------------------------------------------------------------------------

# ACC-02 Get User List

Retrieve list of internal user accounts.

### Endpoint

    GET /api/accounts

### Query Parameters

  Name     Type     Description
  -------- -------- -------------------
  page     int      Page number
  size     int      Page size
  role     string   Filter by role
  status   string   ACTIVE / INACTIVE

### Success Response

``` json
{
  "content": [
    {
      "id": "UUID",
      "fullName": "Nguyen Van A",
      "email": "accountant@g90steel.vn",
      "role": "ACCOUNTANT",
      "status": "ACTIVE",
      "createdAt": "2026-03-13T10:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 100
}
```

------------------------------------------------------------------------

# ACC-03 Get User Detail

Retrieve detailed information for a user account.

### Endpoint

    GET /api/accounts/{id}

### Path Parameters

  Name   Description
  ------ -------------
  id     User ID

### Success Response

``` json
{
  "id": "UUID",
  "fullName": "Nguyen Van A",
  "email": "accountant@g90steel.vn",
  "phone": "0901234567",
  "address": "Ha Noi",
  "role": "ACCOUNTANT",
  "status": "ACTIVE",
  "createdAt": "2026-03-13T10:00:00"
}
```

------------------------------------------------------------------------

# ACC-04 Update User Account

Update role or contact information.

### Endpoint

    PUT /api/accounts/{id}

### Request Body

``` json
{
  "fullName": "Nguyen Van B",
  "phone": "0908888888",
  "address": "Ho Chi Minh",
  "roleId": "UUID_ROLE_WAREHOUSE",
  "status": "ACTIVE"
}
```

### Validation

-   Email cannot be changed
-   Role must be valid
-   Owner cannot change own role

### Success Response

``` json
{
  "message": "User account updated successfully"
}
```

------------------------------------------------------------------------

# ACC-05 Deactivate User Account

Deactivate an existing user account.

### Endpoint

    PATCH /api/accounts/{id}/deactivate

### Request Body

``` json
{
  "reason": "Employee resigned"
}
```

### Behavior

-   Set `status = INACTIVE`
-   Prevent login
-   Keep historical data

### Success Response

``` json
{
  "message": "User account deactivated successfully"
}
```

------------------------------------------------------------------------

# 4. Data Mapping

## users table

  Field           API Field
  --------------- -----------
  id              id
  role_id         roleId
  full_name       fullName
  email           email
  password_hash   password
  phone           phone
  address         address
  status          status
  created_at      createdAt

------------------------------------------------------------------------

# 5. Audit Logging

Every action must create record in:

    audit_logs

Example:

  Field         Value
  ------------- -------------
  action        CREATE_USER
  entity_type   USER
  entity_id     userId
  user_id       ownerId

------------------------------------------------------------------------

# 6. Security Requirements

-   JWT Authentication required
-   Owner role required
-   All APIs must validate permissions
-   Password must be stored as **bcrypt hash**

------------------------------------------------------------------------

# 7. Status Enumeration

Account status values:

    ACTIVE
    INACTIVE
    LOCKED

------------------------------------------------------------------------

# 8. Error Codes

  Code    Meaning
  ------- -----------------------------
  MSG09   Incorrect username/password
  MSG20   Username already exists
  MSG21   Email already exists
  MSG16   Account inactive
