# API Requirement Specification

## Module: Pricing Management (Owner)

### Project

G90 Steel Business Management System

### Source References

-   Project Spec: G90 Steel Business Management System Spec
-   Database Schema: V1\_\_init.sql

This document defines API requirements for **Pricing Management** where
the **Owner** manages price lists and pricing rules.

------------------------------------------------------------------------

# 1. Module Overview

The Pricing Management module allows the **Owner** to manage product
pricing rules.

Main responsibilities:

-   Create price lists
-   Define product pricing for each price list
-   Update price lists
-   Delete price lists
-   View price lists

Roles supported by this module:

  Role         Permission
  ------------ -------------
  Owner        Full access
  Accountant   View only
  Warehouse    No access
  Customer     No access

Database tables involved:

-   `price_lists`
-   `price_list_items`
-   `products`
-   `audit_logs`

------------------------------------------------------------------------

# 2. API List

  ----------------------------------------------------------------------------------
  API ID       API Name              Method            Endpoint
  ------------ --------------------- ----------------- -----------------------------
  PRC-01       Create Price List     POST              /api/price-lists

  PRC-02       Get Price List        GET               /api/price-lists

  PRC-03       Get Price List Detail GET               /api/price-lists/{id}

  PRC-04       Update Price List     PUT               /api/price-lists/{id}

  PRC-05       Delete Price List     DELETE            /api/price-lists/{id}

  PRC-06       Add Price List Item   POST              /api/price-lists/{id}/items

  PRC-07       Update Price List     PUT               /api/price-list-items/{id}
               Item                                    

  PRC-08       Delete Price List     DELETE            /api/price-list-items/{id}
               Item                                    
  ----------------------------------------------------------------------------------

------------------------------------------------------------------------

# 3. API Specifications

------------------------------------------------------------------------

# PRC-01 Create Price List

Create a new price list.

### Endpoint

POST /api/price-lists

### Authorization

Owner

### Request Body

``` json
{
  "name": "Price List 2026",
  "customerGroup": "CONTRACTOR",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE"
}
```

### Validation Rules

-   Name is required
-   Start date must be before end date
-   Status must be ACTIVE or INACTIVE

### Success Response

``` json
{
  "id": "UUID",
  "message": "Price list created successfully"
}
```

------------------------------------------------------------------------

# PRC-02 Get Price List

Retrieve list of price lists.

### Endpoint

GET /api/price-lists

### Query Parameters

  Name            Type     Description
  --------------- -------- --------------------------
  page            int      Page number
  size            int      Page size
  status          string   ACTIVE / INACTIVE
  customerGroup   string   Filter by customer group

### Success Response

``` json
{
  "content": [
    {
      "id": "UUID",
      "name": "Price List 2026",
      "customerGroup": "CONTRACTOR",
      "startDate": "2026-01-01",
      "endDate": "2026-12-31",
      "status": "ACTIVE",
      "createdAt": "2026-03-13T10:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 50
}
```

------------------------------------------------------------------------

# PRC-03 Get Price List Detail

Retrieve detailed information of a price list including product prices.

### Endpoint

GET /api/price-lists/{id}

### Path Parameters

  Name   Description
  ------ ---------------
  id     Price List ID

### Success Response

``` json
{
  "id": "UUID",
  "name": "Price List 2026",
  "customerGroup": "CONTRACTOR",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE",
  "items": [
    {
      "id": "UUID",
      "productId": "UUID",
      "productName": "Steel H Beam",
      "unitPrice": 15000000
    }
  ]
}
```

------------------------------------------------------------------------

# PRC-04 Update Price List

Update price list information.

### Endpoint

PUT /api/price-lists/{id}

### Request Body

``` json
{
  "name": "Updated Price List",
  "customerGroup": "DISTRIBUTOR",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE"
}
```

### Validation

-   Price list must exist
-   Date range must be valid

### Success Response

``` json
{
  "message": "Price list updated successfully"
}
```

------------------------------------------------------------------------

# PRC-05 Delete Price List

Delete a price list.

### Endpoint

DELETE /api/price-lists/{id}

### Behavior

-   Remove price list if not used by active contracts

### Success Response

``` json
{
  "message": "Price list deleted successfully"
}
```

------------------------------------------------------------------------

# PRC-06 Add Price List Item

Add product price to a price list.

### Endpoint

POST /api/price-lists/{id}/items

### Request Body

``` json
{
  "productId": "UUID",
  "unitPrice": 15000000
}
```

### Validation

-   Product must exist
-   Unit price must be positive

### Success Response

``` json
{
  "id": "UUID",
  "message": "Price item added successfully"
}
```

------------------------------------------------------------------------

# PRC-07 Update Price List Item

Update product price.

### Endpoint

PUT /api/price-list-items/{id}

### Request Body

``` json
{
  "unitPrice": 15500000
}
```

### Success Response

``` json
{
  "message": "Price item updated successfully"
}
```

------------------------------------------------------------------------

# PRC-08 Delete Price List Item

Remove product from price list.

### Endpoint

DELETE /api/price-list-items/{id}

### Success Response

``` json
{
  "message": "Price item deleted successfully"
}
```

------------------------------------------------------------------------

# 4. Data Mapping

## price_lists table

  Field            API Field
  ---------------- ---------------
  id               id
  name             name
  customer_group   customerGroup
  start_date       startDate
  end_date         endDate
  status           status
  created_by       createdBy
  created_at       createdAt

------------------------------------------------------------------------

## price_list_items table

  Field           API Field
  --------------- -------------
  id              id
  price_list_id   priceListId
  product_id      productId
  unit_price      unitPrice

------------------------------------------------------------------------

# 5. Audit Logging

All create/update/delete actions must log into:

audit_logs

Example:

  Field         Value
  ------------- -------------------
  action        CREATE_PRICE_LIST
  entity_type   PRICE_LIST
  entity_id     priceListId
  user_id       ownerId

------------------------------------------------------------------------

# 6. Security Requirements

-   JWT Authentication required
-   Only Owner can modify pricing
-   Accountant can view price lists

------------------------------------------------------------------------

# 7. Status Enumeration

Price list status values:

ACTIVE INACTIVE

------------------------------------------------------------------------

# 8. Error Codes

  Code    Meaning
  ------- --------------------
  MSG01   No search results
  MSG08   Exceed max length
  MSG25   Product not found
  MSG50   Invalid date range
