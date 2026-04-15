# Invoice Module Specification

## 1. Scope

Tai lieu nay mo ta hanh vi hien tai cua module Invoice trong codebase, bat dau tu `InvoiceController` va lan sang service, DTO, entity, repository, va cac module tich hop lien quan.

Pham vi chinh:

- API goc: `POST/GET/PUT /api/invoices`
- Chuyen contract thanh invoice: `POST /api/invoices/from-contract/{contractId}`
- Huy invoice: `POST /api/invoices/{invoiceId}/cancel`
- Tich hop xuoi dong/nguoc dong voi:
  - `contracts`
  - `customers`
  - `payment_allocations`
  - `audit_logs`
  - `email`
  - `saleorder`
  - `project`

Tat ca endpoint trong `InvoiceController` deu yeu cau Bearer authentication.

## 2. Data Model

### 2.1 Bang `invoices`

Module payment map bang `invoices` thong qua `PaymentInvoiceEntity`.

Field chinh:

| Field | Y nghia |
|---|---|
| `id` | UUID invoice |
| `invoice_number` | So hoa don, dang `INV-YYYY-NNNN` |
| `contract_id` | Contract/sale order nguon |
| `customer_id` | Khach hang |
| `source_type` | Nguon invoice, hien tai service luon set `CONTRACT` |
| `customer_name` | Snapshot ten khach hang |
| `customer_tax_code` | Snapshot ma so thue |
| `billing_address` | Dia chi xuat hoa don |
| `issue_date` | Ngay xuat hoa don |
| `payment_terms` | Dieu khoan thanh toan |
| `note` | Ghi chu |
| `adjustment_amount` | Dieu chinh cong/tru vao subtotal |
| `total_amount` | Tong truoc VAT sau adjustment |
| `vat_rate` | Ty le VAT |
| `vat_amount` | Tien VAT |
| `status` | Trang thai luu DB |
| `due_date` | Han thanh toan |
| `document_url` | Link tai lieu hoa don, chi cap nhat qua update |
| `created_by`, `updated_by`, `issued_by`, `cancelled_by` | User trace |
| `created_at`, `updated_at`, `issued_at`, `cancelled_at` | Moc thoi gian |
| `notification_sent_at` | Thoi diem gui email thong bao thanh cong |
| `cancellation_reason` | Ly do huy |

### 2.2 Bang `invoice_items`

Module payment map bang `invoice_items` thong qua `PaymentInvoiceItemEntity`.

Field chinh:

| Field | Y nghia |
|---|---|
| `id` | UUID item |
| `invoice_id` | Invoice cha |
| `product_id` | San pham tham chieu, co the null |
| `description` | Mo ta dong hoa don |
| `unit` | Don vi tinh |
| `quantity` | So luong |
| `unit_price` | Don gia |
| `total_price` | Thanh tien dong = `quantity * unit_price` |

### 2.3 Quan he nghiep vu

- Invoice bat buoc gan voi `contract`.
- Contract bat buoc co `customer` moi duoc xuat hoa don.
- Payment history va so tien da thu duoc tinh tu `payment_allocations`.
- Nhieu module cung doc/ghi cung bang `invoices`:
  - payment: CRUD va view invoice
  - debt: cap nhat `status` thanh `PAID` hoac `PARTIALLY_PAID` sau khi phan bo thanh toan
  - project: doc `status` de chan dong project neu invoice chua final

## 3. Authorization

Role duoc xac dinh tu `AuthenticatedUser.role` va map sang `RoleName`.

| Action | Role duoc phep |
|---|---|
| Tao invoice | `ACCOUNTANT`, `OWNER` |
| Convert contract -> invoice | `ACCOUNTANT`, `OWNER` |
| Xem danh sach/detail invoice | `ACCOUNTANT`, `OWNER`, `CUSTOMER` |
| Cap nhat invoice | `ACCOUNTANT`, `OWNER` |
| Huy invoice | `OWNER` |

Rang buoc them:

- `CUSTOMER` chi nhin thay invoice cua chinh customer profile gan voi user dang dang nhap.
- `WAREHOUSE` khong co quyen xem/tao/sua/huy invoice.

## 4. Status Model

### 4.1 Trang thai luu DB

Code hien tai co the gap cac gia tri sau trong `invoices.status`:

- `DRAFT`
- `ISSUED`
- `OPEN`
- `PARTIALLY_PAID`
- `PAID`
- `SETTLED`
- `CANCELLED`
- `VOID`

Luu y:

- `OPEN` duoc normalize thanh `ISSUED` trong payment module.
- `PARTIALLY_PAID` va `PAID` duoc debt module ghi xuong DB sau khi payment allocation duoc tao.
- `SETTLED` va `VOID` duoc payment module nhan dien khi lock/filter/hien thi, nhung `InvoiceController` khong co endpoint nao set 2 trang thai nay.

### 4.2 Trang thai hien thi tren API

Payment module khong chi tra ve trang thai luu DB; no suy dien `status` hien thi theo thanh toan:

- Neu status goc la `DRAFT`, `CANCELLED`, `VOID`, `SETTLED` thi giu nguyen.
- Neu `paidAmount >= grandTotal` va `grandTotal > 0` thi hien thi `PAID`.
- Neu `paidAmount > 0` nhung chua du thi hien thi `PARTIALLY_PAID`.
- Nguoc lai hien thi `ISSUED` hoac status goc da normalize.

### 4.3 Rule chuyen trang thai

- Create chi chap nhan `DRAFT` hoac `ISSUED`.
- Neu create khong truyen `status`, backend default `ISSUED`.
- `OPEN` trong create/update duoc doi thanh `ISSUED`.
- Update chi cho phep invoice dang `DRAFT` hoac `ISSUED` va chua co bat ky payment allocation nao.
- `ISSUED` khong duoc quay nguoc ve `DRAFT`.
- Cancel doi `status` thanh `CANCELLED`.

### 4.4 Mot rang buoc quan trong

Moi contract chi co toi da 1 active invoice theo rule:

- backend khong cho tao invoice moi neu contract da co invoice ma `status` khac `CANCELLED` va `VOID`

He qua thuc te:

- contract da co invoice `PAID` van khong tao duoc invoice moi
- muon tao lai invoice cho cung contract, invoice cu phai o `CANCELLED` hoac `VOID`

## 5. Timezone, Numbering, and Money Rules

### 5.1 Timezone

Tat ca timestamp do entity/service tu tao deu dung `Asia/Ho_Chi_Minh`.

### 5.2 Invoice number

So hoa don duoc sinh theo cong thuc:

- `INV-<year(issueDate)>-<sequence 4 chu so>`

Trong do `sequence` = `count(invoices.issue_date trong nam do) + 1`.

Vi du:

- `INV-2026-0001`
- `INV-2026-0002`

### 5.3 Tinh tien

Cong thuc tinh tien trong payment module:

1. `subtotal = sum(item.totalPrice)`
2. `adjustmentAmount = request.adjustmentAmount`, neu null thi = `0.00`
3. `totalAmount = subtotal + adjustmentAmount`
4. `vatRate = 10.00` neu customer co `taxCode`, nguoc lai `0.00`
5. `vatAmount = totalAmount * vatRate / 100`
6. `grandTotal = totalAmount + vatAmount`
7. `paidAmount` = tong `payment_allocations.amount` cua invoice, cap tren boi `grandTotal`
8. `outstandingAmount = grandTotal - paidAmount`, khong am

Tat ca so tien duoc normalize ve scale 2, `RoundingMode.HALF_UP`.

Rang buoc:

- `totalAmount` sau adjustment phai lon hon `0`

## 6. Endpoint Summary

| Method | Path | Mo ta ngan |
|---|---|---|
| `POST` | `/api/invoices` | Tao invoice tu contract |
| `POST` | `/api/invoices/from-contract/{contractId}` | Alias tao invoice bang contractId tren path |
| `GET` | `/api/invoices` | Danh sach invoice co filter/sort/pagination |
| `GET` | `/api/invoices/{invoiceId}` | Chi tiet invoice |
| `PUT` | `/api/invoices/{invoiceId}` | Cap nhat invoice truoc khi co thanh toan |
| `POST` | `/api/invoices/{invoiceId}/cancel` | Huy invoice |

HTTP status:

- `POST /api/invoices`: `201 Created`
- `POST /api/invoices/from-contract/{contractId}`: `201 Created`
- Cac endpoint con lai: `200 OK`

Success envelope:

```json
{
  "code": "SUCCESS",
  "message": "Invoice created successfully",
  "data": {}
}
```

Error envelope quan trong:

- `VALIDATION_ERROR`
- `INVOICE_NOT_FOUND`
- `FORBIDDEN`

## 7. Endpoint Details

### 7.1 Create Invoice

Endpoint: `POST /api/invoices`

Request body: `InvoiceCreateRequest`

| Field | Type | Required | Rule |
|---|---|---:|---|
| `contractId` | string | Yes | max 36, phai ton tai |
| `issueDate` | date | No | default ngay hien tai theo `Asia/Ho_Chi_Minh` |
| `dueDate` | date | Yes | phai >= `issueDate` |
| `adjustmentAmount` | decimal(18,2) | No | co the am/duong, default `0.00` |
| `billingAddress` | string | No | max 500 |
| `paymentTerms` | string | No | max 255 |
| `note` | string | No | max 1000 |
| `status` | string | No | chi `DRAFT`/`ISSUED`, `OPEN` duoc map thanh `ISSUED`, default `ISSUED` |
| `items` | array | No | neu null hoac `[]`, backend copy tu contract items |

`items[]` khi co custom data:

| Field | Type | Required | Rule |
|---|---|---:|---|
| `productId` | string | No | neu co thi phai thuoc contract |
| `description` | string | Dieu kien | max 255, bat buoc neu khong suy ra duoc tu product |
| `unit` | string | Dieu kien | max 20, bat buoc neu khong suy ra duoc tu product |
| `quantity` | decimal(18,2) | Yes | > 0 |
| `unitPrice` | decimal(18,2) | Yes | > 0 |

Rule nghiep vu:

- User phai la `ACCOUNTANT` hoac `OWNER`.
- Contract phai ton tai va co customer.
- Contract chi duoc xuat invoice khi `status` la `DELIVERED` hoac `COMPLETED`.
- Neu contract da co invoice active (`status` khac `CANCELLED`, `VOID`) thi reject.
- Neu khong truyen `items`, backend lay toan bo `contract_items`.
- Khi lay item tu contract:
  - `quantity` phai > 0
  - neu `unitPrice <= 0` nhung `totalPrice > 0`, backend suy ra `unitPrice = totalPrice / quantity`
  - neu `totalPrice <= 0`, backend suy ra `totalPrice = quantity * unitPrice`
- `billingAddress` fallback theo thu tu: request -> customer.address
- `paymentTerms` fallback theo thu tu: request -> contract.paymentTerms -> customer.paymentTerms
- `customerName`, `customerTaxCode` la snapshot tu customer tai thoi diem tao
- `sourceType` hien tai luon la `CONTRACT`
- Neu tao voi `status = ISSUED`:
  - set `issuedBy`, `issuedAt`
  - ghi audit `ISSUE_INVOICE`
  - neu customer co email thi gui template `invoice-issued`

Response:

- tra ve `InvoiceResponse`
- `paymentHistory` trong response create hien tai la mang rong

### 7.2 Convert Contract To Invoice

Endpoint: `POST /api/invoices/from-contract/{contractId}`

Request body: `ConvertContractToInvoiceRequest`

Body giong `InvoiceCreateRequest` tru truong `contractId`, vi `contractId` lay tu path.

Hanh vi thuc thi:

- controller goi `invoiceService.convertContractToInvoice(contractId, request)`
- service copy body sang `InvoiceCreateRequest`, gan `contractId` tu path, roi delegate ve `createInvoice`

Noi cach khac, day la alias endpoint, khong co logic rieng.

### 7.3 Get Invoice List

Endpoint: `GET /api/invoices`

Query: `InvoiceListQuery`

| Field | Type | Required | Rule |
|---|---|---:|---|
| `keyword` | string | No | max 255, search theo `invoiceNumber`, `customerCode`, `customerName` |
| `invoiceNumber` | string | No | max 50, contains ignore case |
| `customerId` | string | No | max 36, exact match |
| `customerName` | string | No | max 255, contains ignore case |
| `contractId` | string | No | max 36, exact match |
| `status` | string | No | chi chap nhan `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `VOID` |
| `issueFrom` | date | No | inclusive |
| `issueTo` | date | No | inclusive, phai >= `issueFrom` |
| `dueFrom` | date | No | inclusive |
| `dueTo` | date | No | inclusive, phai >= `dueFrom` |
| `page` | int | No | default 1, min 1 |
| `pageSize` | int | No | default 20, min 1, max 100 |
| `sortBy` | string | No | default `issueDate` |
| `sortDir` | string | No | default `desc`, chi `asc`/`desc` |

Sort fields hop le:

- `invoiceNumber`
- `customerName`
- `issueDate`
- `dueDate`
- `grandTotal`
- `outstandingAmount`
- `status`

Quyen xem:

- `ACCOUNTANT`, `OWNER`: thay tat ca invoice
- `CUSTOMER`: chi thay invoice cua chinh minh

Response: `InvoiceListResponseData`

`data.items[]` gom:

- `id`
- `invoiceNumber`
- `sourceType`
- `contractId`
- `contractNumber`
- `customerId`
- `customerCode`
- `customerName`
- `issueDate`
- `dueDate`
- `grandTotal`
- `paidAmount`
- `outstandingAmount`
- `status`
- `documentUrl`

Luu y hien trang:

- `status` trong list la status hien thi da suy dien theo payment allocation
- `totalPages = 0` neu danh sach rong
- API list co the tra ve item co `status = SETTLED`, nhung query `status=SETTLED` lai khong hop le theo validation hien tai

### 7.4 Get Invoice Detail

Endpoint: `GET /api/invoices/{invoiceId}`

Rule:

- `ACCOUNTANT`, `OWNER` duoc xem moi invoice
- `CUSTOMER` chi xem duoc invoice cua customer profile cua minh
- Neu khong tim thay hoac customer truy cap invoice khong thuoc minh, backend tra `INVOICE_NOT_FOUND`

Response: `InvoiceResponse`

Field chinh:

| Field | Y nghia |
|---|---|
| `subtotalAmount` | tong item, hoac `totalAmount - adjustmentAmount` neu khong co item |
| `totalAmount` | tong truoc VAT sau adjustment |
| `grandTotal` | tong sau VAT |
| `paidAmount` | tong da thu tu `payment_allocations` |
| `outstandingAmount` | con phai thu |
| `status` | trang thai hien thi da suy dien |
| `items[]` | chi tiet dong invoice |
| `paymentHistory[]` | lich su allocation/payment |

`items[]` gom:

- `id`
- `productId`
- `productCode`
- `productName`
- `description`
- `unit`
- `quantity`
- `unitPrice`
- `totalPrice`

`paymentHistory[]` gom:

- `paymentId`
- `receiptNumber`
- `paymentDate`
- `allocatedAmount`
- `paymentAmount`
- `paymentMethod`
- `referenceNo`
- `note`
- `createdBy`
- `createdAt`

Chi tiet implementation:

- `paymentHistory` duoc sort giam dan theo `paymentDate`, tiep den `createdAt`
- `receiptNumber` khong luu DB; backend sinh runtime theo dang `RCPT-<8 ky tu dau cua paymentId sau khi bo dau gach>`

### 7.5 Update Invoice

Endpoint: `PUT /api/invoices/{invoiceId}`

Request body: `InvoiceUpdateRequest`

| Field | Type | Required | Rule |
|---|---|---:|---|
| `issueDate` | date | No | voi invoice `ISSUED` thi khong duoc doi |
| `dueDate` | date | No | neu truyen thi phai >= `issueDate` hieu luc |
| `adjustmentAmount` | decimal(18,2) | No | normalize scale 2 |
| `billingAddress` | string | No | max 500 |
| `paymentTerms` | string | No | max 255 |
| `note` | string | No | max 1000 |
| `status` | string | No | chi `DRAFT`/`ISSUED`, `OPEN` -> `ISSUED` |
| `documentUrl` | string | No | max 1000 |
| `items` | array | No | neu co thi replace toan bo items, khong patch tung dong |

Rule nghiep vu:

- User phai la `ACCOUNTANT` hoac `OWNER`.
- Invoice phai ton tai.
- Invoice chi duoc update khi status hien thi hien tai la `DRAFT` hoac `ISSUED`.
- Neu da co bat ky `paidAmount > 0` nao thi khong duoc update.
- `dueDate` hieu luc khong duoc nho hon `issueDate` hieu luc.
- Neu invoice dang `ISSUED`:
  - khong duoc doi `issueDate`
  - khong duoc rut ngan `dueDate` so voi due date hien tai
  - khong duoc doi status nguoc ve `DRAFT`
- Neu `items` khong truyen, backend dung snapshot items hien tai de tinh lai tong tien.
- Neu `items` duoc truyen:
  - phai co it nhat 1 item
  - moi item validate giong create
  - thay the toan bo invoice items
- Neu `grandTotal` moi > `grandTotal cu * 1.05` thi chi `OWNER` moi duoc update.
- Khi update vao `ISSUED` lan dau:
  - set `issuedAt`, `issuedBy` neu truoc do chua co
  - ghi audit `ISSUE_INVOICE` neu transition tu `DRAFT` -> `ISSUED`
- Moi lan update ma status cuoi cung la `ISSUED`:
  - neu customer co email thi gui template `invoice-updated`

Semantics cap nhat field:

- `billingAddress`, `paymentTerms`, `note`, `documentUrl` chi duoc set lai khi field co mat trong request
- Neu client gui chuoi rong, backend normalize ve `null`, nen co the dung de clear field

### 7.6 Cancel Invoice

Endpoint: `POST /api/invoices/{invoiceId}/cancel`

Request body: `InvoiceCancelRequest`

| Field | Type | Required | Rule |
|---|---|---:|---|
| `cancellationReason` | string | Yes | max 1000, khong blank |

Rule nghiep vu:

- Chi `OWNER` duoc huy invoice.
- Khong cho huy neu invoice da `CANCELLED` hoac `VOID`.
- Khong cho huy neu:
  - status hien thi la `PAID`
  - hoac `PARTIALLY_PAID`
  - hoac `SETTLED`
  - hoac `paidAmount > 0`
- Nghia la module Invoice khong tu refund; neu da thu tien thi phai refund o quy trinh khac truoc khi huy.

Tac dong khi huy:

- set `status = CANCELLED`
- set `cancellationReason`
- set `cancelledBy`, `cancelledAt`, `updatedBy`
- ghi audit `CANCEL_INVOICE`
- neu customer co email thi gui template `invoice-cancelled`

## 8. Cross-Module Integration

### 8.1 Sale Order

`SaleOrderServiceImpl#createInvoice` tao `InvoiceCreateRequest` va delegate truc tiep sang `invoiceService.createInvoice`.

He qua:

- endpoint ngoai module payment `POST /api/sale-orders/{saleOrderId}/invoices` van dung toan bo rule cua payment invoice
- `sourceType` trong invoice van la `CONTRACT`, vi sale order hien tai duoc back boi `contracts`

### 8.2 Debt / Payment Allocation

Payment module khong tu luu `paidAmount` trong bang `invoices`; no tong hop tu `payment_allocations`.

Debt module:

- load open invoices theo `customerId`
- sau khi allocate payment, cap nhat `invoices.status` thanh:
  - `PAID` neu remaining = 0
  - `PARTIALLY_PAID` neu con no

Do do:

- `paidAmount`, `outstandingAmount`, `paymentHistory`, `status` hien thi cua invoice phu thuoc truc tiep vao payment allocation
- update/cancel trong payment module se bi chan ngay khi da co payment allocation

### 8.3 Project

Project module doc cung bang `invoices` thong qua `ProjectInvoiceEntity`.

Khi close project, no chan dong neu contract lien ket con invoice co status chua final. Vi vay `invoices.status` anh huong truc tiep den kha nang close project.

### 8.4 Audit Log

Payment invoice ghi audit voi `entityType = INVOICE`.

Action da thay trong code:

- `CREATE_INVOICE`
- `UPDATE_INVOICE`
- `ISSUE_INVOICE`
- `CANCEL_INVOICE`
- `NOTIFY_INVOICE_CUSTOMER_ISSUE`
- `NOTIFY_INVOICE_CUSTOMER_UPDATE`
- `NOTIFY_INVOICE_CUSTOMER_CANCEL`

### 8.5 Email

Neu customer co email, module gui email HTML bang `EmailService`.

Template name hien tai:

- `invoice-issued`
- `invoice-updated`
- `invoice-cancelled`

Bien duoc truyen vao template gom:

- recipient/customer info
- invoice number
- contract number
- issue date
- due date
- grand total
- status
- billing address
- document URL
- cancellation reason
- note
- danh sach items

## 9. Known Current Behaviors and Limitations

- `GET /api/invoices` cho phep filter status toi `VOID`, nhung khong cho `SETTLED`; trong khi response van co the hien `SETTLED`.
- Payment module nhan dien `VOID` va `SETTLED`, nhung `InvoiceController` khong co endpoint chuyen invoice sang 2 status nay.
- Invoice numbering dung `count + 1` theo nam issue date; code hien tai khong co co che lock/tranh collision o muc service.
- Tao/update/cancel invoice co the phu thuoc vao gui email dong bo vi `emailService.sendHtmlEmail(...).join()` duoc goi trong transaction.
- `paymentHistory` chi duoc load day du o endpoint detail; create/update/cancel chi tra mang rong.
- Create voi `items: []` se duoc coi nhu khong truyen item va backend copy item tu contract.
- Update voi `items: []` se bi reject vi backend yeu cau invoice phai co it nhat 1 item khi thay the danh sach.

## 10. Boundary cua Module Invoice

Module Invoice hien tai chiu trach nhiem:

- tao invoice tu contract/sale order da giao hoac hoan tat
- quan ly snapshot thong tin xuat hoa don
- tinh tong tien, VAT, cong no hien thi
- cho phep update truoc khi co thanh toan
- huy invoice khi chua nhan tien
- expose danh sach/detail invoice cho accountant, owner, customer

Module Invoice hien tai khong tu giai quyet:

- refund khi huy invoice da thu tien
- tao payment
- allocate payment vao invoice
- chuyen invoice sang `PAID`/`PARTIALLY_PAID` bang tay
- workflow `VOID`
- workflow `SETTLED`

