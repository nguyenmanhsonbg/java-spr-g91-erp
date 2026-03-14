# Quotation APIs — Request/Response List

This document lists the proposed APIs needed for these screens:
- Quotation Request / Create Quotation
- Quotation Preview / Review
- Quotation List (Customer)
- Quotation Detail

The API proposal is normalized against the current database schema in `01_init.sql` and aligned with the business intent described in `Spec_Project_G90.md`.

---

## 1. Scope and normalization notes

### 1.1 Confirmed by database
From `01_init.sql`, quotation-related persistence currently includes:
- `quotations(id, quotation_number, customer_id, project_id, total_amount, status, valid_until, created_at)`
- `quotation_items(id, quotation_id, product_id, quantity, unit_price, total_price)`
- `products(...)`
- `projects(...)`
- `customers(...)`
- `contracts(... quotation_id ...)`

### 1.2 Confirmed by spec
The spec states quotation-related UI/business needs include:
- quotation number
- creation date
- total amount
- status
- project reference
- delivery requirements
- line items
- promotion code (optional)
- preview/review before submit
- customer quotation list with status filters
- quotation detail as bridge to contract

### 1.3 Gap between DB and spec
The current DB schema does **not** contain these quotation fields directly:
- `delivery_requirements`
- `promotion_code`
- quotation processing history
- quotation notes / audit comments

Because of that, the APIs below are split into two groups:
1. **DB-aligned fields**: directly mapped from current schema.
2. **Spec-driven fields**: included as recommended extension fields. These should be implemented either by:
   - extending `quotations`, or
   - adding related tables such as `quotation_histories`, `quotation_promotions`, or `quotation_metadata`.

---

## 2. Common conventions

### 2.1 Success response envelope
```json
{
  "code": "SUCCESS",
  "message": "Human readable message",
  "data": {}
}
```

### 2.2 Validation error response
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "errors": [
    {
      "field": "items[0].quantity",
      "message": "Quantity must be greater than 0"
    }
  ]
}
```

### 2.3 Permission error response
```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to perform this action"
}
```

### 2.4 Not found response
```json
{
  "code": "NOT_FOUND",
  "message": "Requested resource not found"
}
```

---

## 3. Screen: Quotation Request / Create Quotation

### 3.1 Purpose
Used by **Customer** to create a quotation request from selected products.

### 3.2 APIs needed
1. `GET /api/customer/quotation-form-init`
2. `POST /api/quotations/preview`
3. `POST /api/quotations/draft`
4. `POST /api/quotations/submit`

---

### API 3.2.1 — Load quotation form data
**Method:** `GET`  
**URL:** `/api/customer/quotation-form-init`

#### Purpose
Load data required to render the create quotation screen:
- customer info
- active products for selector
- customer projects
- reference unit prices
- available promotions

#### Request
No request body.

Optional query params:
- `keyword`
- `type`
- `size`
- `thickness`
- `page`
- `pageSize`

Example:
```http
GET /api/customer/quotation-form-init?keyword=ton&type=TON&page=1&pageSize=20
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation form data loaded successfully",
  "data": {
    "customer": {
      "id": "cus_001",
      "companyName": "Công Ty An Phát",
      "customerType": "CONTRACTOR",
      "status": "ACTIVE"
    },
    "products": [
      {
        "id": "prd_001",
        "productCode": "SP000523",
        "productName": "Tôn Mạ Kẽm G90 1.5mm",
        "type": "TON",
        "size": "200x6000",
        "thickness": "1.5",
        "unit": "TAM",
        "referenceWeight": 42.0,
        "status": "ACTIVE",
        "referenceUnitPrice": 32000
      },
      {
        "id": "prd_002",
        "productCode": "SP000524",
        "productName": "Thép Tấm SS400 3.0mm",
        "type": "THEP_TAM",
        "size": "1500x6000",
        "thickness": "3.0",
        "unit": "TAM",
        "referenceWeight": 141.3,
        "status": "ACTIVE",
        "referenceUnitPrice": 850000
      }
    ],
    "projects": [
      {
        "id": "prj_001",
        "projectCode": "PRJ-2026-0001",
        "name": "Nhà xưởng Bắc Ninh",
        "status": "ACTIVE"
      }
    ],
    "availablePromotions": [
      {
        "code": "PROMO10",
        "name": "Giảm 10% cho nhóm Contractor",
        "discountType": "PERCENT",
        "discountValue": 10
      }
    ]
  }
}
```

#### Notes
- `referenceUnitPrice` is not stored directly in `products`; it should be resolved from `price_lists` + `price_list_items` based on customer group and validity.
- `availablePromotions` is spec-driven and must be resolved from `promotions` and business rules.

---

### API 3.2.2 — Preview quotation before save/submit
**Method:** `POST`  
**URL:** `/api/quotations/preview`

#### Purpose
Calculate:
- item totals
- subtotal
- discount/promotion effect
- grand total
- valid-until preview

#### Request
```json
{
  "projectId": "prj_001",
  "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
  "promotionCode": "PROMO10",
  "items": [
    {
      "productId": "prd_001",
      "quantity": 30
    },
    {
      "productId": "prd_002",
      "quantity": 5
    }
  ]
}
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation preview calculated successfully",
  "data": {
    "project": {
      "id": "prj_001",
      "projectCode": "PRJ-2026-0001",
      "name": "Nhà xưởng Bắc Ninh"
    },
    "items": [
      {
        "productId": "prd_001",
        "productCode": "SP000523",
        "productName": "Tôn Mạ Kẽm G90 1.5mm",
        "quantity": 30,
        "unit": "TAM",
        "unitPrice": 32000,
        "totalPrice": 960000
      },
      {
        "productId": "prd_002",
        "productCode": "SP000524",
        "productName": "Thép Tấm SS400 3.0mm",
        "quantity": 5,
        "unit": "TAM",
        "unitPrice": 765000,
        "totalPrice": 3825000
      }
    ],
    "summary": {
      "subTotal": 4785000,
      "discountAmount": 85000,
      "totalAmount": 4700000
    },
    "promotion": {
      "code": "PROMO10",
      "name": "Giảm 10% cho nhóm Contractor",
      "applied": true
    },
    "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
    "validUntil": "2026-03-29",
    "validation": {
      "valid": true,
      "messages": []
    }
  }
}
```

#### Notes
- `deliveryRequirements` and `promotionCode` are not in current quotation schema; they are returned here for UI review consistency.
- `validUntil` maps well to the existing `quotations.valid_until` column.

---

### API 3.2.3 — Save quotation draft
**Method:** `POST`  
**URL:** `/api/quotations/draft`

#### Purpose
Create a quotation in `DRAFT` status.

#### Request
```json
{
  "projectId": "prj_001",
  "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
  "promotionCode": "PROMO10",
  "items": [
    {
      "productId": "prd_001",
      "quantity": 30,
      "unitPrice": 32000
    },
    {
      "productId": "prd_002",
      "quantity": 5,
      "unitPrice": 765000
    }
  ]
}
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation draft saved successfully",
  "data": {
    "quotation": {
      "id": "quo_001",
      "quotationNumber": "QT-20260314-0001",
      "customerId": "cus_001",
      "projectId": "prj_001",
      "totalAmount": 4700000,
      "status": "DRAFT",
      "validUntil": "2026-03-29",
      "createdAt": "2026-03-14T18:30:00+07:00"
    },
    "items": [
      {
        "id": "qit_001",
        "quotationId": "quo_001",
        "productId": "prd_001",
        "quantity": 30,
        "unitPrice": 32000,
        "totalPrice": 960000
      },
      {
        "id": "qit_002",
        "quotationId": "quo_001",
        "productId": "prd_002",
        "quantity": 5,
        "unitPrice": 765000,
        "totalPrice": 3825000
      }
    ],
    "metadata": {
      "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
      "promotionCode": "PROMO10"
    }
  }
}
```

#### DB mapping
- `quotations`: `id`, `quotation_number`, `customer_id`, `project_id`, `total_amount`, `status`, `valid_until`, `created_at`
- `quotation_items`: `id`, `quotation_id`, `product_id`, `quantity`, `unit_price`, `total_price`

#### Extension note
`metadata.deliveryRequirements` and `metadata.promotionCode` require schema extension or separate storage.

---

### API 3.2.4 — Submit quotation
**Method:** `POST`  
**URL:** `/api/quotations/submit`

#### Purpose
Create or finalize a quotation in `PENDING` status.

#### Request
```json
{
  "quotationId": "quo_001"
}
```

Alternative direct-submit request:
```json
{
  "projectId": "prj_001",
  "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
  "promotionCode": "PROMO10",
  "items": [
    {
      "productId": "prd_001",
      "quantity": 30,
      "unitPrice": 32000
    },
    {
      "productId": "prd_002",
      "quantity": 5,
      "unitPrice": 765000
    }
  ]
}
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation submitted successfully",
  "data": {
    "quotation": {
      "id": "quo_001",
      "quotationNumber": "QT-20260314-0001",
      "customerId": "cus_001",
      "projectId": "prj_001",
      "totalAmount": 4700000,
      "status": "PENDING",
      "validUntil": "2026-03-29",
      "createdAt": "2026-03-14T18:30:00+07:00"
    },
    "tracking": {
      "submittedAt": "2026-03-14T18:35:00+07:00",
      "nextAction": "Waiting for accountant review"
    }
  }
}
```

---

## 4. Screen: Quotation Preview / Review

### 4.1 Purpose
Used by **Customer** to review quotation before official submit.

### 4.2 APIs needed
1. `GET /api/quotations/{quotationId}/preview`
2. `PUT /api/quotations/{quotationId}`
3. `POST /api/quotations/{quotationId}/submit`

---

### API 4.2.1 — Get quotation preview/review data
**Method:** `GET`  
**URL:** `/api/quotations/{quotationId}/preview`

#### Purpose
Load the quotation in a review-friendly format before submit.

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation preview loaded successfully",
  "data": {
    "quotation": {
      "id": "quo_001",
      "quotationNumber": "QT-20260314-0001",
      "status": "DRAFT",
      "createdAt": "2026-03-14T18:30:00+07:00",
      "validUntil": "2026-03-29",
      "project": {
        "id": "prj_001",
        "projectCode": "PRJ-2026-0001",
        "name": "Nhà xưởng Bắc Ninh"
      },
      "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
      "promotion": {
        "code": "PROMO10",
        "name": "Giảm 10% cho nhóm Contractor"
      }
    },
    "items": [
      {
        "id": "qit_001",
        "productId": "prd_001",
        "productCode": "SP000523",
        "productName": "Tôn Mạ Kẽm G90 1.5mm",
        "quantity": 30,
        "unit": "TAM",
        "unitPrice": 32000,
        "totalPrice": 960000
      },
      {
        "id": "qit_002",
        "productId": "prd_002",
        "productCode": "SP000524",
        "productName": "Thép Tấm SS400 3.0mm",
        "quantity": 5,
        "unit": "TAM",
        "unitPrice": 765000,
        "totalPrice": 3825000
      }
    ],
    "summary": {
      "subTotal": 4785000,
      "discountAmount": 85000,
      "totalAmount": 4700000
    }
  }
}
```

---

### API 4.2.2 — Edit draft quotation
**Method:** `PUT`  
**URL:** `/api/quotations/{quotationId}`

#### Purpose
Update a quotation while it is still `DRAFT`.

#### Request
```json
{
  "projectId": "prj_001",
  "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 3",
  "promotionCode": "PROMO10",
  "items": [
    {
      "productId": "prd_001",
      "quantity": 25,
      "unitPrice": 32000
    },
    {
      "productId": "prd_002",
      "quantity": 6,
      "unitPrice": 765000
    }
  ]
}
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation updated successfully",
  "data": {
    "quotation": {
      "id": "quo_001",
      "quotationNumber": "QT-20260314-0001",
      "status": "DRAFT",
      "projectId": "prj_001",
      "totalAmount": 5390000,
      "validUntil": "2026-03-29"
    },
    "items": [
      {
        "productId": "prd_001",
        "quantity": 25,
        "unitPrice": 32000,
        "totalPrice": 800000
      },
      {
        "productId": "prd_002",
        "quantity": 6,
        "unitPrice": 765000,
        "totalPrice": 4590000
      }
    ],
    "metadata": {
      "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 3",
      "promotionCode": "PROMO10"
    }
  }
}
```

#### Error response
```json
{
  "code": "QUOTATION_NOT_EDITABLE",
  "message": "Only draft quotation can be edited"
}
```

---

### API 4.2.3 — Submit reviewed quotation
**Method:** `POST`  
**URL:** `/api/quotations/{quotationId}/submit`

#### Request
```json
{}
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation submitted successfully",
  "data": {
    "quotationId": "quo_001",
    "quotationNumber": "QT-20260314-0001",
    "status": "PENDING",
    "submittedAt": "2026-03-14T18:40:00+07:00"
  }
}
```

---

## 5. Screen: Quotation List (Customer)

### 5.1 Purpose
Used by **Customer** to view their own quotations.

### 5.2 APIs needed
1. `GET /api/customer/quotations`
2. `GET /api/customer/quotations/summary`

---

### API 5.2.1 — Get customer quotation list
**Method:** `GET`  
**URL:** `/api/customer/quotations`

#### Purpose
Load paginated customer quotation list with search/filter.

#### Query params
- `keyword`
- `status`
- `fromDate`
- `toDate`
- `page`
- `pageSize`
- `sortBy`
- `sortDir`

Example:
```http
GET /api/customer/quotations?status=PENDING&fromDate=2026-03-01&toDate=2026-03-31&page=1&pageSize=10&sortBy=createdAt&sortDir=desc
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation list fetched successfully",
  "data": {
    "items": [
      {
        "id": "quo_001",
        "quotationNumber": "QT-20260314-0001",
        "createdAt": "2026-03-14T18:30:00+07:00",
        "totalAmount": 4700000,
        "status": "DRAFT",
        "validUntil": "2026-03-29",
        "actions": {
          "canView": true,
          "canEdit": true,
          "canTrack": false
        }
      },
      {
        "id": "quo_002",
        "quotationNumber": "QT-20260314-0002",
        "createdAt": "2026-03-14T18:35:00+07:00",
        "totalAmount": 4700000,
        "status": "PENDING",
        "validUntil": "2026-03-29",
        "actions": {
          "canView": true,
          "canEdit": false,
          "canTrack": true
        }
      },
      {
        "id": "quo_003",
        "quotationNumber": "QT-20260311-0003",
        "createdAt": "2026-03-11T10:15:00+07:00",
        "totalAmount": 8500000,
        "status": "CONVERTED",
        "validUntil": "2026-03-26",
        "actions": {
          "canView": true,
          "canEdit": false,
          "canTrack": true
        }
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 10,
      "totalItems": 3,
      "totalPages": 1
    },
    "filters": {
      "status": "PENDING",
      "fromDate": "2026-03-01",
      "toDate": "2026-03-31"
    }
  }
}
```

#### Notes
- `CONVERTED` and `REJECTED` are spec/UI statuses. They are valid as application-level values for `quotations.status`, since schema currently stores `status` as `VARCHAR(50)`.

---

### API 5.2.2 — Get quotation list summary counts
**Method:** `GET`  
**URL:** `/api/customer/quotations/summary`

#### Purpose
Return counters for tabs/widgets.

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation summary fetched successfully",
  "data": {
    "total": 12,
    "draft": 2,
    "pending": 5,
    "converted": 3,
    "rejected": 2
  }
}
```

---

## 6. Screen: Quotation Detail

### 6.1 Purpose
Used by **Customer** and **Accountant** to view quotation detail.
- Customer: view, edit if draft
- Accountant: review and create contract

### 6.2 APIs needed
1. `GET /api/quotations/{quotationId}`
2. `GET /api/quotations/{quotationId}/history`
3. `POST /api/contracts/from-quotation/{quotationId}`

---

### API 6.2.1 — Get quotation detail
**Method:** `GET`  
**URL:** `/api/quotations/{quotationId}`

#### Purpose
Return full quotation detail for bridge between quotation and contract.

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation detail fetched successfully",
  "data": {
    "quotation": {
      "id": "quo_002",
      "quotationNumber": "QT-20260314-0002",
      "status": "PENDING",
      "totalAmount": 4700000,
      "validUntil": "2026-03-29",
      "createdAt": "2026-03-14T18:35:00+07:00"
    },
    "customer": {
      "id": "cus_001",
      "companyName": "Công Ty An Phát",
      "taxCode": "0102030405",
      "address": "123 Phố Huế, Hai Bà Trưng, Hà Nội",
      "contactPerson": "Nguyễn Văn B",
      "phone": "0987654321",
      "email": "contact@anphat.vn",
      "customerType": "CONTRACTOR"
    },
    "project": {
      "id": "prj_001",
      "projectCode": "PRJ-2026-0001",
      "name": "Nhà xưởng Bắc Ninh",
      "location": "Bắc Ninh",
      "status": "ACTIVE"
    },
    "items": [
      {
        "id": "qit_003",
        "productId": "prd_001",
        "productCode": "SP000523",
        "productName": "Tôn Mạ Kẽm G90 1.5mm",
        "type": "TON",
        "size": "200x6000",
        "thickness": "1.5",
        "unit": "TAM",
        "quantity": 30,
        "unitPrice": 32000,
        "totalPrice": 960000
      },
      {
        "id": "qit_004",
        "productId": "prd_002",
        "productCode": "SP000524",
        "productName": "Thép Tấm SS400 3.0mm",
        "type": "THEP_TAM",
        "size": "1500x6000",
        "thickness": "3.0",
        "unit": "TAM",
        "quantity": 5,
        "unitPrice": 765000,
        "totalPrice": 3825000
      }
    ],
    "pricing": {
      "subTotal": 4785000,
      "discountAmount": 85000,
      "totalAmount": 4700000,
      "promotionCode": "PROMO10"
    },
    "deliveryRequirements": "Giao hàng trong giờ hành chính, tại cổng số 2",
    "actions": {
      "customerCanEdit": false,
      "accountantCanCreateContract": true
    }
  }
}
```

#### Notes
- `deliveryRequirements`, `promotionCode`, and action flags are application-level enrichments.
- Item detail joins `quotation_items` with `products`.
- Customer detail joins `quotations.customer_id -> customers`.
- Project detail joins `quotations.project_id -> projects`.

---

### API 6.2.2 — Get quotation processing history
**Method:** `GET`  
**URL:** `/api/quotations/{quotationId}/history`

#### Purpose
Return handling history for review timeline.

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Quotation history fetched successfully",
  "data": {
    "quotationId": "quo_002",
    "events": [
      {
        "id": "his_001",
        "action": "CREATED",
        "actorRole": "CUSTOMER",
        "actorName": "Công Ty An Phát",
        "note": "Quotation draft created",
        "createdAt": "2026-03-14T18:30:00+07:00"
      },
      {
        "id": "his_002",
        "action": "SUBMITTED",
        "actorRole": "CUSTOMER",
        "actorName": "Công Ty An Phát",
        "note": "Quotation submitted for review",
        "createdAt": "2026-03-14T18:35:00+07:00"
      },
      {
        "id": "his_003",
        "action": "REVIEWING",
        "actorRole": "ACCOUNTANT",
        "actorName": "Tran Thi Accountant",
        "note": "Assigned for review",
        "createdAt": "2026-03-14T19:00:00+07:00"
      }
    ]
  }
}
```

#### Note
Current DB does not have a dedicated quotation history table. This endpoint requires either:
- a new `quotation_histories` table, or
- deriving events from `audit_logs` if quotation actions are logged there.

---

### API 6.2.3 — Create contract from quotation
**Method:** `POST`  
**URL:** `/api/contracts/from-quotation/{quotationId}`

#### Purpose
Used by **Accountant** to convert quotation into contract.

#### Request
```json
{
  "paymentTerms": "70% on delivery, 30% within 30 days",
  "deliveryAddress": "KCN Quế Võ, Bắc Ninh"
}
```

#### Response
```json
{
  "code": "SUCCESS",
  "message": "Contract created from quotation successfully",
  "data": {
    "contract": {
      "id": "ctr_001",
      "contractNumber": "CT-20260314-0001",
      "customerId": "cus_001",
      "quotationId": "quo_002",
      "totalAmount": 4700000,
      "status": "DRAFT",
      "paymentTerms": "70% on delivery, 30% within 30 days",
      "deliveryAddress": "KCN Quế Võ, Bắc Ninh",
      "createdAt": "2026-03-14T19:15:00+07:00"
    },
    "quotation": {
      "id": "quo_002",
      "quotationNumber": "QT-20260314-0002",
      "status": "CONVERTED"
    }
  }
}
```

#### DB mapping
- Creates a new row in `contracts`
- Creates rows in `contract_items` copied from `quotation_items`
- Updates `quotations.status = 'CONVERTED'`

---

## 7. Minimal endpoint list

```http
GET  /api/customer/quotation-form-init
POST /api/quotations/preview
POST /api/quotations/draft
POST /api/quotations/submit

GET  /api/quotations/{quotationId}/preview
PUT  /api/quotations/{quotationId}
POST /api/quotations/{quotationId}/submit

GET  /api/customer/quotations
GET  /api/customer/quotations/summary

GET  /api/quotations/{quotationId}
GET  /api/quotations/{quotationId}/history
POST /api/contracts/from-quotation/{quotationId}
```

---

## 8. Recommended schema extensions

To fully support the UI/spec cleanly, add these fields or related tables:

### Option A — Extend `quotations`
- `delivery_requirements TEXT`
- `promotion_code VARCHAR(50)`
- `updated_at TIMESTAMP`
- `submitted_at TIMESTAMP NULL`

### Option B — Add related tables
- `quotation_histories`
- `quotation_promotions`
- `quotation_metadata`

This would make the API contract cleaner and reduce reliance on derived or temporary fields.
