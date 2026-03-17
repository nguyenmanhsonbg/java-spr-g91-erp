# Huong Dan Su Dung Module Contract Management

## 1. Muc dich

Tai lieu nay huong dan cach su dung module Quan ly Bao gia va Hop dong cho tung role trong he thong G90 Steel.

Pham vi module hien tai bao gom:

- tao va quan ly quotation theo flow customer
- chuyen quotation thanh contract
- tao contract thu cong cho giao dich offline
- submit, cancel, track va phe duyet contract
- tao, xem, export va email document metadata cua contract

## 2. Dieu kien su dung

- Nguoi dung phai dang nhap qua `POST /api/auth/login`
- Sau khi dang nhap, gui header:

```http
Authorization: Bearer <accessToken>
```

- Swagger UI co the gan Bearer token truc tiep qua nut `Authorize`
- Tat ca timestamp duoc xu ly theo mui gio Viet Nam `GMT+7`

## 3. Vong doi nghiep vu

### 3.1. Quotation

Trang thai quotation hien tai:

- `DRAFT`
- `PENDING`
- `APPROVED`
- `REJECTED`
- `CONVERTED`

Nguyen tac:

- quotation moi tao chinh thuc se vao `PENDING`
- quotation luu tam se vao `DRAFT`
- quotation hop le trong 15 ngay lich
- quotation da chuyen sang contract se vao `CONVERTED`
- customer chi duoc xem quotation cua chinh minh

### 3.2. Contract

Trang thai contract hien tai:

- `DRAFT`
- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`
- `SUBMITTED`
- `PROCESSING`
- `RESERVED`
- `PICKED`
- `IN_TRANSIT`
- `DELIVERED`
- `COMPLETED`
- `CANCELLED`

Trang thai approval hien tai:

- `NOT_REQUIRED`
- `PENDING`
- `APPROVED`
- `REJECTED`
- `MODIFICATION_REQUESTED`

Nguyen tac:

- chi `DRAFT` moi duoc sua tu do
- contract tri gia lon hoac can override gia se vao flow approval
- submit contract se check inventory truoc
- contract `COMPLETED`, `DELIVERED`, `CANCELLED` khong duoc cancel lai
- lich su version, status history va audit log duoc giu de truy vet

## 4. Ma tran quyen

### Customer

Co the:

- preview quotation
- tao quotation
- luu quotation draft
- cap nhat draft quotation cua minh
- submit draft quotation cua minh
- xem danh sach quotation cua minh
- xem chi tiet, preview va history quotation cua minh
- xem danh sach contract cua minh
- xem chi tiet contract cua minh
- xem tracking cua contract cua minh
- xem danh sach document cua contract cua minh
- export hoac email document neu contract khong confidential

Khong the:

- tao contract
- sua, submit hoac cancel contract
- generate document moi
- truy cap quotation/contract cua customer khac
- phe duyet contract

### Accountant

Co the:

- xem toan bo quotation
- xem chi tiet, history quotation
- preview contract
- tao contract thu cong
- tao contract tu quotation
- xem toan bo contract
- sua draft contract
- submit contract
- cancel contract
- theo doi tracking contract
- generate, export, email document contract

Khong the:

- approve, reject hoac request modification cho contract can owner duyet

### Owner

He thong hien tai cho phep `OWNER` nhu admin phe duyet.

Co the:

- xem pending approvals
- approve contract
- reject contract
- request modification
- thuc hien cac API operational giong `ACCOUNTANT`

## 5. Huong dan cho Customer

### 5.1. Preview quotation truoc khi luu

API:

- `POST /api/quotations/preview`

Nhap:

- `projectId` neu quotation gan voi du an cu the
- `note`
- `deliveryRequirements`
- `promotionCode` neu co
- `items`

Moi item gom:

- `productId`
- `quantity`

Business rule:

- toi da 20 dong hang
- tong quotation submit chinh thuc phai tu `10,000,000 VND`
- san pham phai dang `ACTIVE`
- he thong tu tinh gia theo pricing cua nhom customer va promotion hop le neu co

Khuyen nghi:

- dung preview de kiem tra gia, tong tien va warning truoc khi luu that

### 5.2. Tao quotation chinh thuc

API:

- `POST /api/quotations`

Ket qua:

- quotation vao `PENDING`
- `validUntil` duoc set 15 ngay

Nen dung khi:

- da chot danh sach hang va muon gui de accountant xu ly tiep

### 5.3. Luu quotation nhap tam

API:

- `POST /api/quotations/draft`

Dung khi:

- can luu tam quotation de bo sung sau
- chua san sang submit chinh thuc

### 5.4. Cap nhat draft quotation

API:

- `PUT /api/quotations/{quotationId}`

Chi ap dung cho:

- quotation dang `DRAFT`
- quotation thuoc customer dang dang nhap

### 5.5. Submit draft quotation

API:

- `POST /api/quotations/{quotationId}/submit`
- `POST /api/quotations/submit`

Dung khi:

- muon submit lai draft da tao truoc
- hoac submit theo payload action flow

### 5.6. Xem danh sach quotation cua minh

API:

- `GET /api/quotations`

Filter ho tro:

- `keyword`
- `quotationNumber`
- `status`
- `fromDate`, `toDate`
- `page`, `pageSize`
- `sortBy`, `sortDir`

Hanh vi:

- backend tu dong gioi han theo customer dang dang nhap

### 5.7. Xem chi tiet quotation

API:

- `GET /api/quotations/{quotationId}`
- `GET /api/quotations/{quotationId}/preview`
- `GET /api/quotations/{quotationId}/history`

Nen kiem tra:

- `status`
- `validUntil`
- danh sach item va tong tien
- lich su submit/update/chuyen contract

### 5.8. Xem contract cua minh

API:

- `GET /api/contracts`
- `GET /api/contracts/{contractId}`

Filter contract ho tro:

- `keyword`
- `contractNumber`
- `status`
- `approvalStatus`
- `createdFrom`, `createdTo`
- `deliveryFrom`, `deliveryTo`
- `confidential`
- `submitted`
- `page`, `pageSize`
- `sortBy`, `sortDir`

Hanh vi:

- customer chi thay contract cua chinh minh
- hop dong confidential van xem duoc detail co ban, nhung action document se bi gioi han boi policy

### 5.9. Track contract

API:

- `GET /api/contracts/{contractId}/tracking`

Theo doi:

- status hien tai
- timeline
- milestone timestamp
- expected vs actual dates
- tracking number neu co

### 5.10. Xem va xu ly document contract

API:

- `GET /api/contracts/{contractId}/documents`
- `POST /api/contracts/{contractId}/documents/{documentId}/export`
- `POST /api/contracts/{contractId}/documents/{documentId}/email`

Loai document hien co:

- `SALES_CONTRACT`
- `PROFORMA_INVOICE`
- `DELIVERY_NOTE`
- `PACKING_LIST`

Luu y:

- customer khong duoc generate document moi
- document cua contract confidential co the bi chan export/email
- draft document co watermark `DRAFT`

## 6. Huong dan cho Accountant

### 6.1. Xem quotation toan he thong

API:

- `GET /api/quotations`

Filter ho tro:

- `keyword`
- `quotationNumber`
- `customerId`
- `status`
- `fromDate`, `toDate`
- `page`, `pageSize`
- `sortBy`, `sortDir`

Dung khi:

- tim quotation cho xu ly
- loc quotation pending
- tra cuu quotation cua tung customer

### 6.2. Xem chi tiet va lich su quotation

API:

- `GET /api/quotations/{quotationId}`
- `GET /api/quotations/{quotationId}/preview`
- `GET /api/quotations/{quotationId}/history`

Can kiem tra truoc khi convert:

- quotation con hop le
- customer hop le
- item, gia va tong tien dung nghiep vu

### 6.3. Preview contract

API:

- `POST /api/contracts/preview`

Nhap:

- `customerId`
- `quotationId` neu tao tu quotation
- `paymentTerms`
- `deliveryAddress`
- `deliveryTerms`
- `note`
- `expectedDeliveryDate`
- `confidential`
- `items`

Moi item gom:

- `productId`
- `quantity`
- `unitPrice`
- `priceOverrideReason` neu co

Ket qua preview tra ve:

- tong tien
- base price va final price tung dong
- `requiresApproval`
- `approvalTier`
- `creditLimit`, `currentDebt`, `projectedDebt`
- `depositPercentage`, `depositAmount`
- danh sach `warnings`

Business rule can nho:

- customer phai ton tai va dang active
- gia khong duoc thap hon 90% base price
- gia khac base price se duoc danh dau approval-ready
- contract tren `500,000,000 VND` se vao flow owner approval
- he thong canh bao neu du no vuot credit limit hoac ton kho khong du

### 6.4. Tao contract thu cong

API:

- `POST /api/contracts`

Dung cho:

- giao dich offline
- tao contract khong can quotation dau vao

Khuyen nghi:

- co the su dung payment terms chuan `70% on delivery, 30% within 30 days`
- xem `depositPercentage` trong preview:
  - customer moi: `30%`
  - customer co lich su tu 6 thang tro len: `20%`

### 6.5. Tao contract tu quotation

API:

- `POST /api/contracts/from-quotation/{quotationId}`

Nhap:

- `paymentTerms`
- `deliveryAddress`

Hanh vi:

- contract tu dong lay customer va item tu quotation
- quotation duoc danh dau `CONVERTED` sau khi tao contract thanh cong

### 6.6. Xem danh sach contract

API:

- `GET /api/contracts`

Filter ho tro:

- `keyword`
- `contractNumber`
- `customerId`
- `status`
- `approvalStatus`
- `createdFrom`, `createdTo`
- `deliveryFrom`, `deliveryTo`
- `confidential`
- `submitted`
- `page`, `pageSize`
- `sortBy`, `sortDir`

Nen dung de:

- tim contract draft chua submit
- loc contract dang cho owner duyet
- loc contract confidential

### 6.7. Xem chi tiet contract

API:

- `GET /api/contracts/{contractId}`

Man hinh chi tiet tra ve:

- thong tin contract
- thong tin customer
- quotation lien ket neu co
- item va thong so san pham
- approval data
- credit data
- version history
- status history
- documents

Can kiem tra:

- `status`
- `approvalStatus`
- `pendingAction`
- `requiresApproval`
- `creditData`

### 6.8. Cap nhat draft contract

API:

- `PUT /api/contracts/{contractId}`

Bat buoc:

- `changeReason`

Chi duoc sua khi:

- contract dang `DRAFT`

Co the sua:

- danh sach item
- so luong
- gia
- delivery address
- delivery terms
- payment terms
- note
- expected delivery date
- confidential flag

Business rule:

- khong doi `customerId` sau khi da tao draft
- doi gia se duoc tinh lai approval-ready flag
- moi lan sua deu tao version moi va audit log

### 6.9. Submit contract

API:

- `POST /api/contracts/{contractId}/submit`

Payload ho tro:

- `scheduledSubmissionAt`
- `submissionNote`

Submit chi thanh cong khi:

- da co `paymentTerms`
- da co `deliveryAddress`
- co it nhat 1 item
- customer dang `ACTIVE`
- khong co pending approval khac
- inventory check pass
- credit limit khong bi vuot

Ket qua:

- neu khong can approval: contract vao `SUBMITTED`, goi hook reserve inventory va notify warehouse
- neu can approval: contract vao `PENDING_APPROVAL`, tao approval request cho owner

Luu y:

- field `autoSubmitDueAt` dang o muc scheduler-ready cho co che auto submit sau 7 ngay

### 6.10. Cancel contract

API:

- `POST /api/contracts/{contractId}/cancel`

Nhap:

- `cancellationReason`
- `cancellationNote`

Ly do hop le:

- `CUSTOMER_REQUEST`
- `PRICE_DISPUTE`
- `INVENTORY_SHORTAGE`
- `CREDIT_RISK`
- `DATA_ERROR`
- `OTHER`

Chi duoc cancel khi:

- contract chua `COMPLETED`
- contract chua `DELIVERED`
- contract chua `CANCELLED`
- chua co invoicing activity dang hoat dong
- khong co approval dang pending

Ket qua:

- contract tren `100,000,000 VND` se tao approval request de owner xu ly
- contract du dieu kien cancel truc tiep se vao `CANCELLED`

### 6.11. Theo doi contract

API:

- `GET /api/contracts/{contractId}/tracking`

Theo doi duoc:

- current status
- timeline event
- inventory reservation event
- approval event
- document event
- shipment tracking number neu co

### 6.12. Tao va xuat document contract

API:

- `GET /api/contracts/{contractId}/documents`
- `POST /api/contracts/{contractId}/documents/generate`
- `POST /api/contracts/{contractId}/documents/{documentId}/export`
- `POST /api/contracts/{contractId}/documents/{documentId}/email`

Request generate:

- `documentType`
- `officialDocument`

Quy tac:

- neu contract van `DRAFT`, he thong chi tao preview document
- official document duoc danh so tuan tu theo ngay
- draft document co watermark `DRAFT`
- moi lan generate/export/email deu duoc log

Luu y van hanh:

- gateway PDF/email hien la extension point. He thong da luu metadata, document number, export count va email timestamp, nhung pipeline gui email/file storage thuc te can duoc noi them neu can production full.

## 7. Huong dan cho Owner

### 7.1. Xem danh sach contract cho phe duyet

API:

- `GET /api/contracts/approvals/pending`

Filter ho tro:

- `page`
- `pageSize`

Nen uu tien kiem tra:

- contract tri gia tren `500,000,000 VND`
- contract co `approvalStatus = PENDING`
- contract co `pendingAction = SUBMIT` hoac `CANCEL`

### 7.2. Approve contract

API:

- `POST /api/contracts/{contractId}/approve`

Payload:

- `comment`

Ket qua:

- neu dang cho duyet submit: contract vao `SUBMITTED`, inventory duoc reserve qua integration hook
- neu dang cho duyet cancel: contract vao `CANCELLED`

### 7.3. Reject contract

API:

- `POST /api/contracts/{contractId}/reject`

Ket qua:

- submit approval bi reject se dua contract ve `DRAFT`
- cancel approval bi reject se giu trang thai van hanh truoc do

### 7.4. Request modification

API:

- `POST /api/contracts/{contractId}/request-modification`

Dung khi:

- can accountant sua lai noi dung hop dong truoc khi phe duyet

Ket qua:

- contract ve `DRAFT`
- approval status chuyen sang `MODIFICATION_REQUESTED`

## 8. Luu y nghiep vu quan trong

- khong co hard delete cho quotation va contract
- moi action quan trong deu co audit log va status history
- quotation toi da 20 line items
- quotation submit chinh thuc phai dat toi thieu `10,000,000 VND`
- contract gia thap hon 90% base price se bi chan
- contract tri gia lon hon `500,000,000 VND` bat buoc vao owner approval
- cancel contract lon hon `100,000,000 VND` se vao approval path
- customer debt va credit limit duoc xet khi preview/create/submit contract
- inventory check bat buoc truoc submit
- customer chi duoc truy cap du lieu cua chinh minh
- contract confidential co stricter document access cho customer

## 9. Trinh tu thao tac de xuat

### Quy trinh Customer

1. Preview quotation.
2. Luu draft neu chua chot.
3. Submit quotation chinh thuc.
4. Theo doi quotation cua minh.
5. Khi da co contract, mo detail va tracking.
6. Export hoac email document da duoc phat hanh neu policy cho phep.

### Quy trinh Accountant

1. Loc quotation `PENDING`.
2. Mo chi tiet quotation va quyet dinh convert.
3. Preview contract de kiem tra gia, credit va ton kho.
4. Tao contract thu cong hoac tao tu quotation.
5. Neu can, cap nhat draft voi `changeReason`.
6. Submit contract.
7. Theo doi pending approval neu contract can owner duyet.
8. Generate document sau khi contract o trang thai phu hop.

### Quy trinh Owner

1. Mo danh sach pending approvals.
2. Xem chi tiet contract.
3. Approve, reject hoac request modification.

## 10. Loi thuong gap

- `FORBIDDEN`: sai role hoac customer dang truy cap du lieu khong thuoc minh
- `CONTRACT_NOT_FOUND`: sai `contractId` hoac khong co quyen xem
- `QUOTATION_NOT_FOUND`: sai `quotationId` hoac khong co quyen xem
- `CONTRACT_NOT_EDITABLE`: contract khong con o `DRAFT`
- `CONTRACT_SUBMIT_NOT_ALLOWED`: thieu payment terms, delivery address, item, customer inactive hoac approval dang pending
- `CONTRACT_CREDIT_LIMIT_EXCEEDED`: du no sau khi tao/submit vuot credit limit
- `CONTRACT_INVENTORY_UNAVAILABLE`: ton kho khong du de submit
- `VALIDATION_ERROR`: thieu field bat buoc, qua 20 items, gia khong hop le, email sai dinh dang

## 11. Ghi chu cho doi van hanh

- Khi demo nhanh tren Swagger, dang nhap qua `/api/auth/login`, copy `accessToken`, sau do dung `Authorize`
- `GET /api/quotations` va `GET /api/contracts` da co phan quyen theo role o backend, frontend khong can tu loc thay cho server
- Notification warehouse, email va auto job hien dang o muc extension point. API van tra ket qua va log metadata, nhung muon co tich hop thuc te can noi them gateway ben ngoai
