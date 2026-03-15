# Huong Dan Su Dung Module Project Construction Management

## 1. Muc dich

Tai lieu nay huong dan cach su dung module Quan ly Du an Xay dung cho tung role trong he thong G90 Steel.

Module nay ho tro:

- tao du an xay dung gan voi 1 khach hang cu the
- theo doi tien do, milestone, kho phu trach va tong quan tai chinh
- archive, restore, close du an theo dung business rule

## 2. Dieu kien su dung

- Nguoi dung phai dang nhap qua `POST /api/auth/login`
- Sau khi dang nhap, gui header:

```http
Authorization: Bearer <accessToken>
```

- Swagger UI co the gan Bearer token truc tiep qua nut `Authorize`
- Tat ca thoi gian duoc tinh theo mui gio Viet Nam `GMT+7`

## 3. Vong doi du an

Trang thai chinh cua du an:

- `DRAFT`
- `ACTIVE`
- `ON_HOLD`
- `COMPLETED`
- `CLOSED`
- `ARCHIVED`

Nguyen tac:

- du an `CLOSED` hoac `ARCHIVED` khong duoc sua van hanh
- archive la xoa mem, khong xoa vat ly
- du an archived co cua so restore 30 ngay
- close chi hop le khi milestone, no, hoa don, don hang va sign-off da du dieu kien

## 4. Ma tran quyen

### Accountant

Co the:

- tao du an
- xem danh sach tat ca du an
- xem chi tiet du an
- cap nhat thong tin du an
- archive du an
- restore du an trong recovery window
- gan doi kho chinh va kho backup
- them va cap nhat tien do
- xem milestone
- xem tong quan tai chinh
- close du an

Khong the:

- confirm milestone voi tu cach customer

### Customer

Co the:

- xem danh sach du an cua chinh minh
- xem chi tiet du an cua chinh minh
- xem milestone cua chinh minh
- confirm milestone cua chinh minh

Khong the:

- tao, sua, archive, restore, close du an
- xem financial summary
- truy cap du an cua customer khac

### Owner

He thong hien tai cho phep `OWNER` nhu admin override trong security.

Co the:

- thuc hien cac API operational giong `ACCOUNTANT`

Luu y:

- day la quyen override ky thuat de van hanh he thong
- nghiep vu chinh van xem `ACCOUNTANT` la role quan ly lifecycle du an

### Warehouse

Module Project hien tai khong mo API operational truc tiep cho `WAREHOUSE`.

Role nay khong co cac thao tac rieng trong module nay.

## 5. Huong dan cho Accountant

### 5.1. Tao du an

API:

- `POST /api/projects`

Can nhap:

- `customerId`
- `name`
- `location`
- `scope`
- `startDate`
- `endDate`
- `budget`
- `assignedProjectManager`
- `primaryWarehouseId` neu da biet
- `backupWarehouseId` neu can
- `linkedContractId` neu co
- `linkedOrderReference` neu co
- `paymentMilestones`

Business rule can nho:

- moi du an bat buoc thuoc dung 1 customer ton tai
- toi thieu 3 payment milestones
- `assignedProjectManager` bat buoc
- `startDate <= endDate`
- ma du an tu dong sinh theo format `PRJ-YYYY-XXXX`

Khuyen nghi:

- luon tao du milestone 30, 60, 100 hoac theo thuc te nghiem thu
- neu du an can theo doi vat tu ngay tu dau, gan kho chinh luc tao

### 5.2. Xem danh sach du an

API:

- `GET /api/projects`

Filter ho tro:

- `projectCode`
- `projectName`
- `customerId`
- `status`
- `progressStatus`
- `warehouseId`
- `assignedManager`
- `createdFrom`, `createdTo`
- `startFrom`, `startTo`
- `endFrom`, `endTo`
- `archived`
- `page`, `pageSize`, `sortBy`, `sortDir`

Dung khi:

- tra cuu nhanh du an dang chay
- loc du an cham tien do
- loc du an da archive

### 5.3. Xem chi tiet du an

API:

- `GET /api/projects/{projectId}`

Man hinh chi tiet tra ve:

- thong tin co ban
- timeline
- budget va chi phi
- milestone
- tai lieu metadata
- associated orders placeholder
- delivery history placeholder
- payment status
- progress updates
- kho duoc gan

Nen kiem tra:

- `weeklyUpdateOverdue`
- `behindSchedule`
- `outstandingBalance`
- `archiveApprovalStatus`
- `budgetApprovalStatus`

### 5.4. Cap nhat du an

API:

- `PUT /api/projects/{projectId}`

Bat buoc:

- `changeReason`

Co the sua:

- ten du an
- dia diem
- scope
- ngay bat dau, ket thuc
- budget
- project manager
- kho
- thong tin tham chieu contract/order
- milestone neu chua vao luong confirm
- cac so lieu tong hop tai chinh ho tro

Business rule:

- khong sua du an `CLOSED` hoac `ARCHIVED`
- tang ngan sach hon 10% se dat `budgetApprovalStatus = APPROVAL_READY`
- budget khong duoc nho hon `actualSpend` hoac `commitments`
- milestone khong duoc thay moi neu da vao luong xac nhan

### 5.5. Gan kho cho du an

API:

- `POST /api/projects/{projectId}/warehouses`

Nhap:

- `primaryWarehouseId`
- `backupWarehouseId` neu can
- `assignmentReason`

Business rule:

- kho phai ton tai
- kho backup khong duoc trung kho chinh
- lich su gan kho duoc luu trong `project_warehouse_assignments`

### 5.6. Cap nhat tien do

API:

- `POST /api/projects/{projectId}/progress`
- `PUT /api/projects/{projectId}/progress/{progressUpdateId}`

Nhap:

- `progressPercent`
- `progressStatus`
- `phase`
- `notes`
- `changeReason` neu tien do giam
- `evidenceDocuments`

Business rule:

- `progressPercent` phai trong khoang 0 den 100
- giam tien do bat buoc co `changeReason`
- neu progress kich hoat milestone readiness thi phai co evidence
- chi cho sua ban ghi progress moi nhat
- he thong track duoc weekly overdue va behind schedule

Khuyen nghi:

- cap nhat it nhat moi 7 ngay
- luon day len metadata hinh anh/chung tu tien do cho cac moc quan trong

### 5.7. Archive du an

API:

- `PATCH /api/projects/{projectId}/archive`

Nhap:

- `reason`

Chi duoc archive khi:

- khong co transaction tai chinh
- khong co active contract dependency
- khong co linked order dang mo

Ket qua:

- `status = ARCHIVED`
- dat `restoreDeadline = archivedAt + 30 ngay`
- dat `archiveApprovalStatus = APPROVAL_READY`

### 5.8. Restore du an

API:

- `POST /api/projects/{projectId}/restore`

Chi restore duoc khi:

- du an dang `ARCHIVED`
- van nam trong recovery window 30 ngay

### 5.9. Xem tong quan tai chinh

API:

- `GET /api/projects/{projectId}/financial-summary`

Tra ve:

- budget
- actual spend
- commitments
- variance
- breakdown by category
- payments received
- payments due
- outstanding balance
- profitability amount
- profitability margin

Luu y:

- mot so so lieu hien dang tong hop tu truong du an va diem mo rong hien co
- co the duoc nang cap tiep khi debt/invoice/reporting module day du hon

### 5.10. Close du an

API:

- `POST /api/projects/{projectId}/close`

Nhap:

- `closeReason`
- `customerSignoffCompleted`
- `customerSatisfactionScore`
- `warrantyStartDate`
- `warrantyEndDate`

Chi duoc close khi:

- tat ca milestone da `CONFIRMED`, `AUTO_CONFIRMED` hoac `COMPLETED`
- khong con open orders
- khong con issue chua giai quyet
- invoice/payment da settle
- `outstandingBalance = 0`
- customer da sign-off

Ket qua:

- `status = CLOSED`
- khong cho phep sua van hanh tiep
- bat dau metadata warranty

## 6. Huong dan cho Customer

### 6.1. Xem danh sach du an cua minh

API:

- `GET /api/projects`

Hanh vi:

- he thong tu dong gioi han du an theo `customer` cua user dang nhap
- neu co truyen `customerId` khac, request se bi tu choi

Nen dung de:

- theo doi du an dang thuc hien
- xem du an da hoan tat hay da close
- tim du an theo ma, ten, trang thai

### 6.2. Xem chi tiet du an cua minh

API:

- `GET /api/projects/{projectId}`

Customer co the xem:

- thong tin du an
- tien do
- milestone
- cac cap nhat moi nhat
- trang thai thanh toan tong quan
- kho phuc vu du an

Customer khong xem duoc:

- financial summary API rieng

### 6.3. Xem milestone

API:

- `GET /api/projects/{projectId}/milestones`

Khi xem milestone, can chu y:

- `status`
- `confirmationStatus`
- `confirmationDeadline`
- `autoConfirmEligible`
- `paymentReleaseReady`

### 6.4. Confirm milestone

API:

- `POST /api/projects/{projectId}/milestones/{milestoneId}/confirm`

Chi duoc confirm khi:

- milestone thuoc du an cua chinh customer do
- milestone dang o `READY_FOR_CONFIRMATION`
- milestone chua auto-confirm hoac confirm truoc do

Quy tac:

- cua so confirm la 7 ngay tinh tu luc milestone san sang xac nhan
- qua 7 ngay, he thong co the auto-confirm theo co che scheduler-ready

Khuyen nghi:

- confirm som sau khi da kiem tra hoc muc hoan thanh
- neu co van de nghiem thu, lien he accountant truoc khi het cua so 7 ngay

## 7. Luu y nghiep vu quan trong

- Khong co hard delete trong module nay
- Tat ca thao tac quan trong deu duoc ghi audit log
- Trang thai `APPROVAL_READY` la co san cho workflow phe duyet sau nay, chua phai approval engine day du
- `OWNER` co the thao tac nhu admin, nhung user guide van xem `ACCOUNTANT` la role van hanh chinh
- Customer chi nhin thay project cua minh, service co kiem tra ownership o backend

## 8. Trinh tu thao tac de xuat

### Quy trinh Accountant

1. Tao du an.
2. Gan kho chinh va kho backup neu can.
3. Cap nhat tien do hang tuan, kem evidence.
4. Theo doi milestone da san sang xac nhan.
5. Theo doi financial summary va outstanding balance.
6. Archive neu du an bi huy va khong phat sinh giao dich.
7. Close khi da du dieu kien tai chinh va sign-off.

### Quy trinh Customer

1. Dang nhap va xem danh sach du an cua minh.
2. Mo chi tiet du an de theo doi tien do va milestone.
3. Xac nhan milestone trong cua so 7 ngay khi da nghiem thu.

## 9. Loi thuong gap

- `FORBIDDEN`: sai role hoac customer dang truy cap project khong thuoc minh
- `PROJECT_NOT_FOUND`: sai `projectId` hoac project khong ton tai
- `PROJECT_ARCHIVE_NOT_ALLOWED`: du an van con dependency/giao dich
- `PROJECT_CLOSE_NOT_ALLOWED`: milestone, no, hoa don, order hoac sign-off chua du dieu kien
- `VALIDATION_ERROR`: thieu field bat buoc, date range sai, milestone < 3, budget khong hop le, progress giam khong co ly do

## 10. Ghi chu cho doi van hanh

- Neu can test nhanh bang Swagger, dang nhap qua `/api/auth/login`, copy `accessToken`, sau do dung nut `Authorize`
- Neu can demo cho customer, chi nen su dung cac API `GET /api/projects`, `GET /api/projects/{id}`, `GET /api/projects/{id}/milestones`, `POST /api/projects/{id}/milestones/{milestoneId}/confirm`
- Neu can demo cho accountant, nen chuan bi san `customerId`, `warehouseId` va bo 3 milestone de flow tao du an chay tron
