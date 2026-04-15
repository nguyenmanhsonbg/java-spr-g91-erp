# Payment Module Frontend Specification

## 1. Muc tieu

Tai lieu nay mo ta frontend specification cho module thanh toan chuyen khoan thu cong theo flow:

1. Customer xem chi tiet invoice.
2. Customer xem thong tin chuyen khoan + QR.
3. Customer gui payment confirmation request.
4. Accountant/Owner review request.
5. Accountant/Owner confirm hoac reject.
6. Khi confirm, backend tu tao payment + allocation va cap nhat invoice/debt theo logic hien tai.

Module nay khong dung payment gateway, khong co webhook, khong co callback tu ngan hang.

## 2. Roles va pham vi giao dien

- `CUSTOMER`
  - Xem invoice cua chinh minh
  - Xem huong dan chuyen khoan
  - Tao payment confirmation request
  - Xem danh sach request cua invoice cua minh
  - Xem chi tiet request cua minh
- `ACCOUNTANT`
  - Xem toan bo payment confirmation request
  - Xem chi tiet request
  - Confirm request
  - Reject request
- `OWNER`
  - Giong `ACCOUNTANT`
- `WAREHOUSE`
  - Khong duoc phep vao flow nay

## 3. Quy uoc chung cho Frontend

### 3.1 Response envelope

Tat ca API deu tra theo format:

```json
{
  "code": "SUCCESS",
  "message": "...",
  "data": {}
}
```

Khi loi validation hoac business rule, backend tra:

```json
{
  "code": "REQUEST_VALIDATION_ERROR",
  "message": "...",
  "errors": [
    {
      "field": "requestedAmount",
      "message": "requestedAmount must be greater than 0"
    }
  ]
}
```

### 3.2 Timezone va format

- Timezone nghiep vu: `Asia/Ho_Chi_Minh`
- `LocalDate`: gui/nhan theo format `YYYY-MM-DD`
- `LocalDateTime`: gui/nhan theo ISO format, vi du `2026-04-15T09:30:00`

### 3.3 Money

- Hien thi 2 chu so thap phan
- Lam tron `HALF_UP`
- Cac field tien te co the parse ve number/decimal o frontend, khong xu ly bang integer cents

### 3.4 Status su dung trong UI

#### Invoice status lien quan

- `ISSUED`
- `PARTIALLY_PAID`
- `PAID`
- `CANCELLED`
- `VOID`

Frontend chi cho phep tao payment confirmation request khi:

- `invoice.status` nam trong `ISSUED`, `PARTIALLY_PAID`
- `invoice.outstandingAmount > 0`

#### Payment confirmation request status

- `PENDING_REVIEW`
- `CONFIRMED`
- `REJECTED`
- `CANCELLED`

## 4. Danh sach API can dung

## 4.1 Lay chi tiet invoice

- Method: `GET`
- URL: `/api/invoices/{invoiceId}`
- Roles: `ACCOUNTANT`, `OWNER`, `CUSTOMER` (own only)
- Muc dich FE:
  - Load man chi tiet invoice
  - Lay `grandTotal`, `paidAmount`, `outstandingAmount`, `status`
  - Hien lich su payment da allocate

### Field quan trong trong `data`

- `id`
- `invoiceNumber`
- `customerId`
- `customerName`
- `issueDate`
- `dueDate`
- `grandTotal`
- `paidAmount`
- `outstandingAmount`
- `status`
- `items`
- `paymentHistory`

### FE note

- Nut "Xac nhan thanh toan" chi hien thi khi invoice hop le theo rule o muc 3.4
- Can luu `customerId`, `invoiceNumber`, `outstandingAmount` de dung o man payment

## 4.2 Lay huong dan thanh toan

- Method: `GET`
- URL: `/api/invoices/{invoiceId}/payment-instruction`
- Roles: `ACCOUNTANT`, `OWNER`, `CUSTOMER` (own only)
- Muc dich FE:
  - Hien thong tin ngan hang
  - Hien noi dung chuyen khoan
  - Hien QR

### Response `data`

```json
{
  "paymentMethod": "BANK_TRANSFER_QR",
  "invoiceId": "uuid",
  "invoiceNumber": "INV-2026-0001",
  "customerId": "uuid",
  "grandTotal": 1000.00,
  "paidAmount": 200.00,
  "outstandingAmount": 800.00,
  "bankName": "G90 Bank",
  "bankAccountName": "G90 Steel",
  "bankAccountNo": "0000000000",
  "transferContent": "PAY-INV-2026-0001",
  "qrContent": "BANK_TRANSFER_QR|bankName=...|accountName=...|accountNumber=...|amount=800.00|content=PAY-INV-2026-0001",
  "qrImageUrl": null
}
```

### FE note

- `transferContent` phai co nut copy
- `qrImageUrl` hien tai co the `null`
- Frontend phai uu tien render QR tu `qrImageUrl` neu co
- Neu `qrImageUrl = null` thi frontend tu render QR tu `qrContent`

## 4.3 Tao payment confirmation request

- Method: `POST`
- URL: `/api/invoices/{invoiceId}/payment-confirmation-requests`
- Roles: `CUSTOMER`
- Muc dich FE:
  - Submit thong tin customer da chuyen khoan

### Request body

```json
{
  "requestedAmount": 800.00,
  "transferTime": "2026-04-15T09:30:00",
  "senderBankName": "ACB",
  "senderAccountName": "NGUYEN VAN A",
  "senderAccountNo": "0123456789",
  "referenceCode": "REF-001",
  "proofDocumentUrl": "https://example.com/proof.png",
  "note": "Da chuyen khoan theo noi dung yeu cau"
}
```

### Validation can enforce o FE

- `requestedAmount` > 0
- Toi da 2 chu so thap phan
- `requestedAmount <= outstandingAmount`
- `transferTime` bat buoc
- `senderBankName` bat buoc, max 255
- `senderAccountName` bat buoc, max 255
- `senderAccountNo` bat buoc, max 100
- `referenceCode` bat buoc, max 100
- `proofDocumentUrl` max 1000
- `note` max 1000

### Business rule FE can check som

- Khong cho submit neu invoice `PAID`, `CANCELLED`, `VOID`
- Khong cho submit neu da ton tai 1 request `PENDING_REVIEW` cho invoice

### Response `data`

Tra ve `PaymentConfirmationRequestResponse`. Xem shape day du o muc 4.5.

## 4.4 Lay danh sach request theo invoice

- Method: `GET`
- URL: `/api/invoices/{invoiceId}/payment-confirmation-requests`
- Roles:
  - `CUSTOMER`: chi request cua invoice cua minh
  - `ACCOUNTANT`, `OWNER`: xem duoc
- Muc dich FE:
  - Hien tab "Lich su xac nhan thanh toan" trong man invoice detail
  - Kiem tra co `PENDING_REVIEW` de khoa nut tao moi

### Query params

- `keyword`
- `status`
- `createdFrom`
- `createdTo`
- `page`
- `pageSize`
- `sortBy`
- `sortDir`

### `sortBy` hop le

- `createdAt`
- `transferTime`
- `requestedAmount`
- `status`
- `reviewedAt`
- `invoiceNumber`
- `customerName`

### `sortDir` hop le

- `asc`
- `desc`

## 4.5 Lay chi tiet request

- Method: `GET`
- URL: `/api/payment-confirmation-requests/{requestId}`
- Roles:
  - `CUSTOMER`: own only
  - `ACCOUNTANT`, `OWNER`: all
- Muc dich FE:
  - Trang review detail
  - Drawer/modal detail

### Response `data`

```json
{
  "id": "uuid",
  "invoiceId": "uuid",
  "invoiceNumber": "INV-2026-0001",
  "customerId": "uuid",
  "customerCode": "C001",
  "customerName": "Alpha Steel",
  "requestedAmount": 800.00,
  "confirmedAmount": null,
  "transferTime": "2026-04-15T09:30:00",
  "senderBankName": "ACB",
  "senderAccountName": "NGUYEN VAN A",
  "senderAccountNo": "0123456789",
  "referenceCode": "REF-001",
  "proofDocumentUrl": "https://example.com/proof.png",
  "note": "Da chuyen khoan",
  "status": "PENDING_REVIEW",
  "reviewNote": null,
  "reviewedBy": null,
  "reviewedAt": null,
  "createdBy": "user-id",
  "updatedBy": "user-id",
  "createdAt": "2026-04-15T09:31:00",
  "updatedAt": "2026-04-15T09:31:00",
  "paymentId": null,
  "invoiceGrandTotal": 1000.00,
  "invoicePaidAmount": 200.00,
  "invoiceOutstandingAmount": 800.00,
  "invoiceStatus": "ISSUED"
}
```

### FE note

- `reviewedBy` hien tai la user id, khong phai display name
- `paymentId` co gia tri sau khi confirm
- Man review nen luon hien `invoiceOutstandingAmount` moi nhat truoc khi reviewer thao tac

## 4.6 Lay danh sach request tong

- Method: `GET`
- URL: `/api/payment-confirmation-requests`
- Roles: `ACCOUNTANT`, `OWNER`, `CUSTOMER`
- Muc dich FE:
  - `ACCOUNTANT`, `OWNER`: man danh sach review tong
  - `CUSTOMER`: man lich su request cua customer hien tai

### Query params

- `keyword`
- `invoiceId`
- `customerId`
- `status`
- `createdFrom`
- `createdTo`
- `page`
- `pageSize`
- `sortBy`
- `sortDir`

### Response `data`

```json
{
  "items": [
    {
      "id": "uuid",
      "invoiceId": "uuid",
      "invoiceNumber": "INV-2026-0001",
      "customerId": "uuid",
      "customerCode": "C001",
      "customerName": "Alpha Steel",
      "requestedAmount": 800.00,
      "confirmedAmount": null,
      "transferTime": "2026-04-15T09:30:00",
      "senderBankName": "ACB",
      "senderAccountName": "NGUYEN VAN A",
      "senderAccountNo": "0123456789",
      "referenceCode": "REF-001",
      "proofDocumentUrl": "https://example.com/proof.png",
      "status": "PENDING_REVIEW",
      "reviewNote": null,
      "reviewedBy": null,
      "reviewedAt": null,
      "paymentId": null,
      "createdAt": "2026-04-15T09:31:00"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 1,
    "totalPages": 1
  },
  "filters": {
    "keyword": null,
    "invoiceId": null,
    "customerId": null,
    "status": "PENDING_REVIEW"
  }
}
```

### FE note

- `CUSTOMER` khong can cho nhap `customerId`, backend se tu ep ve customer hien tai
- O man reviewer, nen default filter `status=PENDING_REVIEW`

## 4.7 Confirm request

- Method: `POST`
- URL: `/api/payment-confirmation-requests/{requestId}/confirm`
- Roles: `ACCOUNTANT`, `OWNER`
- Muc dich FE:
  - Reviewer xac nhan da doi soat dung giao dich

### Request body

```json
{
  "confirmedAmount": 800.00,
  "reviewNote": "Da doi soat voi sao ke ngan hang"
}
```

### Validation FE

- `confirmedAmount` bat buoc
- `confirmedAmount > 0`
- toi da 2 chu so thap phan
- `confirmedAmount <= invoiceOutstandingAmount`
- `reviewNote` max 1000

### FE note

- Chi hien nut confirm khi `status = PENDING_REVIEW`
- Neu reviewer mo detail lau, can reload detail truoc khi submit de tranh outstanding thay doi
- Sau confirm, invoice se duoc backend cap nhat qua allocation logic, frontend khong tu set status

## 4.8 Reject request

- Method: `POST`
- URL: `/api/payment-confirmation-requests/{requestId}/reject`
- Roles: `ACCOUNTANT`, `OWNER`
- Muc dich FE:
  - Reviewer tu choi request

### Request body

```json
{
  "reason": "Khong tim thay giao dich phu hop"
}
```

### Validation FE

- `reason` bat buoc
- `reason` max 1000

### FE note

- Chi hien nut reject khi `status = PENDING_REVIEW`
- Reject khong tao payment, khong thay doi debt

## 5. Man hinh can xay dung

## 5.1 Customer - Invoice Detail Payment Block

### Muc tieu

Them mot block trong man `Invoice Detail` hien:

- Invoice summary
- Outstanding amount
- Bank transfer instruction
- QR code
- Danh sach payment confirmation request theo invoice
- CTA tao request moi

### Data loading sequence

1. Goi `GET /api/invoices/{invoiceId}`
2. Neu invoice co the thanh toan, goi `GET /api/invoices/{invoiceId}/payment-instruction`
3. Goi `GET /api/invoices/{invoiceId}/payment-confirmation-requests?sortBy=createdAt&sortDir=desc&page=1&pageSize=20`

### UI section de xay dung

1. Invoice summary
   - Invoice number
   - Customer name
   - Grand total
   - Paid amount
   - Outstanding amount
   - Invoice status
2. Transfer instruction
   - Bank name
   - Account name
   - Account number
   - Transfer content
   - Copy buttons
3. QR area
   - Neu `qrImageUrl` co gia tri: hien image
   - Neu `qrImageUrl` null: render QR tu `qrContent`
4. Action area
   - Nut `Gui xac nhan thanh toan`
   - Disabled khi da co request `PENDING_REVIEW`
5. Request history table
   - Requested amount
   - Transfer time
   - Reference code
   - Status
   - Reviewed at
   - Payment id
   - Action xem chi tiet

## 5.2 Customer - Create Payment Confirmation Modal/Page

### Fields

- `requestedAmount`
- `transferTime`
- `senderBankName`
- `senderAccountName`
- `senderAccountNo`
- `referenceCode`
- `proofDocumentUrl`
- `note`

### UX flow

1. Prefill:
   - `requestedAmount = invoice.outstandingAmount`
   - `transferTime = now` theo timezone local user, nhung gui len dang ISO
2. Validate client-side
3. Submit `POST /api/invoices/{invoiceId}/payment-confirmation-requests`
4. Sau khi success:
   - Dong modal hoac redirect
   - Reload request list theo invoice
   - Reload invoice detail
   - Hien toast thanh cong

### Luu y

- `proofDocumentUrl` hien tai la text input URL, khong phai upload file
- Neu sau nay co upload module rieng, FE co the doi field nay thanh upload + lay URL

## 5.3 Customer - Request Detail

### Muc tieu

Cho phep customer xem:

- Thong tin da gui
- Trang thai review
- Ghi chu reject/confirm
- Payment id neu da confirm

### Hanh vi

1. Goi `GET /api/payment-confirmation-requests/{requestId}`
2. Hien timeline:
   - Created
   - Pending review
   - Confirmed hoac Rejected
3. Hien invoice current summary:
   - `invoiceGrandTotal`
   - `invoicePaidAmount`
   - `invoiceOutstandingAmount`
   - `invoiceStatus`

## 5.4 Accountant/Owner - Review List

### Muc tieu

Tao man danh sach tong de doi soat request.

### Suggested filters

- Keyword
- Status
- Date range
- Invoice id
- Customer id

### Suggested columns

- Created at
- Invoice number
- Customer code
- Customer name
- Requested amount
- Transfer time
- Reference code
- Status
- Reviewed at
- Payment id
- Action xem chi tiet

### Data loading

`GET /api/payment-confirmation-requests?status=PENDING_REVIEW&page=1&pageSize=20&sortBy=createdAt&sortDir=desc`

## 5.5 Accountant/Owner - Review Detail

### Muc tieu

Reviewer xem day du request va thuc hien confirm/reject.

### Layout goi y

1. Request summary
2. Invoice summary
3. Transfer information
4. Proof section
5. Review action panel

### Review action panel

- Confirm form
  - `confirmedAmount`
  - `reviewNote`
  - Nut `Confirm`
- Reject form
  - `reason`
  - Nut `Reject`

### Hanh vi sau action

Sau `confirm` hoac `reject`:

1. Reload chi tiet request
2. Reload chi tiet invoice neu page co hien
3. Update list row status
4. Disable action buttons

## 6. Luong thao tac Frontend chi tiet

## 6.1 Luong customer thanh toan

1. Customer vao invoice detail.
2. FE goi invoice detail.
3. FE kiem tra:
   - `status` co nam trong `ISSUED`, `PARTIALLY_PAID`
   - `outstandingAmount > 0`
4. Neu hop le, FE goi payment instruction.
5. FE goi request list theo invoice.
6. FE hien bank info, QR, transfer content.
7. Customer nhan `Gui xac nhan thanh toan`.
8. FE mo modal form.
9. Customer nhap thong tin giao dich.
10. FE validate client-side.
11. FE submit create request.
12. Neu success:
    - show toast
    - dong modal
    - reload invoice detail
    - reload request list
13. Neu da co `PENDING_REVIEW`, FE an hoac disable CTA tao moi.

## 6.2 Luong reviewer confirm

1. Reviewer vao man review list.
2. FE goi list API voi filter `PENDING_REVIEW`.
3. Reviewer mo detail request.
4. FE goi request detail.
5. FE hien invoice outstanding moi nhat.
6. Reviewer nhap `confirmedAmount` va `reviewNote`.
7. FE validate `confirmedAmount <= invoiceOutstandingAmount`.
8. FE submit confirm API.
9. Neu success:
   - reload request detail
   - reload list
   - neu co invoice page lien ket, reload invoice detail
10. FE hien status `CONFIRMED`, payment id, invoice balance moi.

## 6.3 Luong reviewer reject

1. Reviewer vao request detail.
2. Reviewer nhap ly do reject.
3. FE submit reject API.
4. Neu success:
   - reload detail
   - reload list
5. FE hien status `REJECTED` va `reviewNote`.

## 7. Logic hien thi va khoa nut

### 7.1 Customer

- Hien nut `Gui xac nhan thanh toan` khi:
  - role = `CUSTOMER`
  - invoice status la `ISSUED` hoac `PARTIALLY_PAID`
  - `outstandingAmount > 0`
  - khong co request `PENDING_REVIEW`
- An nut neu invoice `PAID`, `CANCELLED`, `VOID`
- Request list la read-only, khong co edit/delete

### 7.2 Accountant/Owner

- Hien nut `Confirm` va `Reject` khi request `PENDING_REVIEW`
- An hoac disable action khi request da `CONFIRMED`, `REJECTED`, `CANCELLED`
- Sau khi confirm, hien `paymentId`

## 8. Error handling de Frontend map

### 8.1 Validation errors

Map `errors[].field` vao form field tuong ung.

Vi du:

- `requestedAmount`
- `transferTime`
- `senderBankName`
- `senderAccountName`
- `senderAccountNo`
- `referenceCode`
- `proofDocumentUrl`
- `note`
- `confirmedAmount`
- `reviewNote`
- `reason`

### 8.2 Business errors thuong gap

- Invoice khong hop le de gui request
- Invoice khong con outstanding amount
- Da ton tai request `PENDING_REVIEW`
- Request khong con o trang thai `PENDING_REVIEW`
- Confirm amount vuot outstanding amount
- User khong duoc phep truy cap

Frontend nen:

- Hien message tu `message`
- Neu co `errors`, hien tai field
- Neu la 403/forbidden, redirect hoac hien empty state phu hop

## 9. State management goi y

- Tach state theo 3 khoi:
  - `invoiceDetail`
  - `paymentInstruction`
  - `paymentConfirmationRequests`
- Sau khi create request:
  - invalidate `invoiceDetail`
  - invalidate `paymentConfirmationRequests`
- Sau khi confirm/reject:
  - invalidate `paymentConfirmationRequests`
  - invalidate `invoiceDetail`

Neu dung React Query hoac SWR, nen dung cache key:

- `["invoice", invoiceId]`
- `["invoice-payment-instruction", invoiceId]`
- `["invoice-payment-requests", invoiceId, query]`
- `["payment-confirmation-request", requestId]`
- `["payment-confirmation-requests", query]`

## 10. Known backend limitations can the hien tren FE

- Chua co upload file proof rieng, hien tai chi nhan `proofDocumentUrl`
- Chua co `qrImageUrl` thuc te, frontend nen render tu `qrContent`
- `reviewedBy`, `createdBy`, `updatedBy` la id, khong phai display name
- Khong co action edit request sau khi tao
- Khong co action cancel request tu frontend trong version hien tai

## 11. De xuat thu tu implement Frontend

1. Xay block payment trong man invoice detail
2. Tich hop payment instruction API
3. Tich hop create payment confirmation request modal
4. Tich hop request history theo invoice
5. Xay review list cho `ACCOUNTANT` va `OWNER`
6. Xay review detail + confirm/reject action
7. Bo sung validation, loading state, empty state, error state

## 12. Checklist test UI

- Customer thay duoc bank info va QR tren invoice con no
- Customer copy duoc transfer content
- Customer tao request thanh cong
- Customer khong tao duoc request neu da co `PENDING_REVIEW`
- Customer xem duoc lich su request theo invoice
- Reviewer thay duoc list `PENDING_REVIEW`
- Reviewer confirm request thanh cong
- Reviewer reject request thanh cong
- Sau confirm, invoice detail cap nhat `paidAmount`, `outstandingAmount`, `status`
- Role khong hop le khong thay action button
