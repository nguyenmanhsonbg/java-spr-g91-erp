# Huong Dan Tai Hien Flow Customer Tao Quotation -> Accountant Tao Contract -> Owner Phe Duyet

## 1. Muc dich

Tai lieu nay huong dan cac buoc thao tac tren UI de tai hien day du flow:

1. Customer tao quotation
2. Accountant tiep nhan quotation va tao contract
3. Owner phe duyet contract

Tai lieu nay duoc viet theo he thong hien tai. Neu UI dang dung tu `Admin`, can map dung vao role `OWNER`.

## 2. Dieu kien truoc khi test

### 2.1. He thong can san sang

- Database da migrate xong
- Backend dang chay
- Frontend dang chay
- Tai khoan seed mac dinh da ton tai

### 2.2. Tai khoan de demo

Neu DB dang dung seed mac dinh, co the su dung:

- Customer: `customer@g90steel.vn` / `admin`
- Accountant: `accountant@g90steel.vn` / `admin`
- Owner: `admin@g90steel.vn` / `admin`

### 2.3. Du lieu toi thieu can co

Can co it nhat cac du lieu sau truoc khi tai hien flow:

- 1 customer active
- 1 bang gia active cho nhom customer `CONTRACTOR`
- it nhat 2 san pham active trong bang gia

San pham seed co san co the dung:

- `SP000001` - Ton ma kem G90 0.45mm
- `SP000002` - Ton lanh mau xanh 0.50mm
- `SP000003` - Thep tam SS400 3.0mm
- `SP000004` - Thep tam SS400 4.0mm

Luu y quan trong:

- Neu chua co bang gia active, customer se khong tao duoc quotation hop le
- Neu muon di het flow phe duyet Owner, contract can roi vao approval path

### 2.4. Cach dam bao contract roi vao Owner approval

De chac chan tai hien duoc buoc Owner approval, nen chuan bi 1 trong 2 cach:

- cach 1: tong contract lon hon `500,000,000 VND`
- cach 2: accountant thay doi gia so voi base price de contract bi danh dau can approval

Khuyen nghi de demo de nhat:

- tao bang gia sao cho tong quotation va tong contract vuot `500,000,000 VND`

## 3. Buoc 0 - Chuan bi bang gia de chay flow

Neu he thong da co bang gia active, bo qua buoc nay.

### Vai tro

- `OWNER`

### Cac buoc tren UI

1. Dang nhap bang tai khoan `admin@g90steel.vn`.
2. Mo module Price List.
3. Tao 1 bang gia moi cho nhom customer `CONTRACTOR`.
4. Chon ngay hieu luc hop le va dat trang thai `ACTIVE`.
5. Them it nhat 2 san pham active, nen dung `SP000003` va `SP000004` hoac 2 san pham bat ky de frontend de tim.
6. Dat don gia sao cho tong contract sau nay de vuot `500,000,000 VND`.
7. Luu bang gia.

### Ket qua mong doi

- Customer nhom `CONTRACTOR` nhin thay quotation preview co gia hop le
- Accountant preview/create contract khong bi loi thieu pricing

## 4. Buoc 1 - Customer tao quotation

### Vai tro

- `CUSTOMER`

### Man hinh lien quan

- Login
- Product Catalog
- Product Detail
- Quotation Request
- Quotation Preview
- Quotation List
- Quotation Detail

### Cac buoc tren UI

1. Dang nhap bang tai khoan `customer@g90steel.vn`.
2. Mo Product Catalog.
3. Tim san pham da co trong bang gia, vi du `SP000003` va `SP000004`.
4. Mo Product Detail de kiem tra quy cach neu can.
5. Chon `Add to Quotation` hoac mo man Quotation Request.
6. Trong Quotation Request, nhap:
   - danh sach san pham
   - so luong cho tung dong
   - `project reference` neu UI co
   - `delivery requirements`
   - `note`
   - `promotion code` neu muon test promotion
7. Bam `Preview`.
8. Kiem tra quotation preview:
   - tong tien lon hon `10,000,000 VND`
   - item dung san pham
   - delivery requirements dung
9. Neu muon test luu nhap, bam `Save Draft`.
10. Neu muon di flow chinh, bam `Submit`.
11. Mo Quotation List de kiem tra quotation vua tao.
12. Mo Quotation Detail de kiem tra lai du lieu.

### Ket qua mong doi

- Neu `Save Draft`: quotation co trang thai `DRAFT`
- Neu `Submit`: quotation co trang thai `PENDING`
- Quotation chi xuat hien trong danh sach quotation cua customer dang dang nhap

### Checkpoint QA

- Customer khong thay quotation cua customer khac
- Preview tra ve gia va tong tien hop le
- Sau submit, quotation xuat hien o list va detail dung du lieu vua nhap

## 5. Buoc 2 - Accountant tiep nhan quotation

### Vai tro

- `ACCOUNTANT`

### Man hinh lien quan

- Login
- Quotation List
- Quotation Detail / Review

### Cac buoc tren UI

1. Dang xuat tai khoan customer.
2. Dang nhap bang tai khoan `accountant@g90steel.vn`.
3. Mo Quotation List.
4. Loc theo:
   - status `PENDING`
   - customer neu can
   - khoang ngay tao neu can
5. Tim quotation customer vua submit.
6. Mo Quotation Detail / Review.
7. Kiem tra:
   - customer info
   - item list
   - tong tien
   - delivery requirements
   - note cua customer

### Ket qua mong doi

- Accountant xem duoc quotation vua tao
- Action tao contract xuat hien tren quotation hop le

## 6. Buoc 3 - Accountant tao contract tu quotation

### Vai tro

- `ACCOUNTANT`

### Man hinh lien quan

- Create Contract
- Contract Preview
- Contract Detail

### Cac buoc tren UI

1. Tai Quotation Detail, bam `Create Contract` hoac `Copy to Contract`.
2. Neu UI goi API init form, he thong se preload:
   - customer info
   - current debt
   - credit limit
   - item tu quotation
   - payment terms de xuat
3. Trong man Create Contract, kiem tra:
   - customer dung
   - item da copy dung
   - delivery address
   - delivery terms
   - payment terms
4. Neu can, chinh sua item, so luong, don gia.
5. Bam `Preview Contract`.
6. Kiem tra man Preview:
   - tong contract
   - `requiresApproval`
   - credit snapshot
   - warning neu co
7. Bam `Save Draft` hoac `Create Contract`.
8. Mo Contract Detail.

### Ket qua mong doi

- Contract duoc tao thanh cong
- Contract o trang thai `DRAFT`
- Quotation lien quan duoc danh dau `CONVERTED`

### Checkpoint QA

- Contract number duoc sinh tu dong
- Customer, item, total amount va terms dung theo du lieu vua tao
- Contract detail hien version history ban dau

## 7. Buoc 4 - Accountant cap nhat contract neu can

### Vai tro

- `ACCOUNTANT`

### Khi nao can buoc nay

- Muon test chinh sua draft
- Muon test rule `changeReason`
- Muon co thay doi gia de contract di vao approval path

### Cac buoc tren UI

1. Tai Contract Detail, bam `Update Contract`.
2. Thay doi 1 trong cac thong tin:
   - item
   - quantity
   - unit price
   - delivery address
   - delivery terms
   - payment terms
   - note
3. Nhap `changeReason`.
4. Luu lai.
5. Mo lai Contract Detail.

### Ket qua mong doi

- Contract van o `DRAFT`
- Version history tang them 1 ban ghi
- Audit va status history duoc cap nhat o backend

## 8. Buoc 5 - Accountant submit contract de dua vao flow Owner

### Vai tro

- `ACCOUNTANT`

### Cac buoc tren UI

1. Tai Contract Detail, kiem tra contract da du:
   - payment terms
   - delivery address
   - it nhat 1 item
2. Xac nhan tong contract dang di dung nhanh muon test:
   - neu muon Owner approval: lon hon `500,000,000 VND` hoac co approval flag
3. Bam `Submit`.
4. Neu UI cho nhap note submit, nhap `submission note`.
5. Luu va cho he thong xu ly.

### Ket qua mong doi

- Neu contract khong can approval: contract sang `SUBMITTED`
- Neu contract can approval: contract sang `PENDING_APPROVAL`

### De tai hien dung flow nay

Can dam bao contract vao:

- `status = PENDING_APPROVAL`
- `approvalStatus = PENDING`

### Checkpoint QA

- Pending approval xuat hien trong role Owner
- Accountant khong duoc approve contract bang chinh tai khoan cua minh

## 9. Buoc 6 - Owner xem danh sach contract cho phe duyet

### Vai tro

- `OWNER`

### Man hinh lien quan

- Pending Approvals

### Cac buoc tren UI

1. Dang xuat tai khoan accountant.
2. Dang nhap bang tai khoan `admin@g90steel.vn`.
3. Mo man Pending Approvals.
4. Loc theo:
   - customer
   - requested date
   - amount range
   - pending action neu UI co
5. Tim contract vua duoc accountant submit.

### Ket qua mong doi

- Contract xuat hien trong pending list
- List hien duoc it nhat cac thong tin:
   - contract number
   - customer
   - total amount
   - requested by
   - submitted date
   - approval status

## 10. Buoc 7 - Owner review contract truoc khi quyet dinh

### Vai tro

- `OWNER`

### Man hinh lien quan

- Contract Review for Approval

### Cac buoc tren UI

1. Tai Pending Approvals, mo contract vua chon.
2. Tren man review, kiem tra:
   - contract detail
   - item list
   - payment terms
   - delivery terms
   - credit risk / debt snapshot
   - version history
   - ly do can approval
3. Xac dinh nhanh test mong muon:
   - Approve
   - Reject
   - Request Modification

### Ket qua mong doi

- Owner nhin thay day du du lieu de ra quyet dinh
- UI phai lam ro ly do can approval, khong chi hien moi status

## 11. Buoc 8 - Test nhanh Approve

### Cac buoc tren UI

1. Bam `Approve`.
2. Nhap comment neu UI yeu cau.
3. Confirm.

### Ket qua mong doi

- Contract chuyen sang `SUBMITTED`
- Approval status chuyen sang `APPROVED`
- Tracking co them event approval
- Accountant va Customer co the thay trang thai moi khi reload man hinh

## 12. Buoc 9 - Test nhanh Reject

Neu khong muon dung contract vua approve, co the tao lai 1 quotation/contract moi va lap lai den buoc review.

### Cac buoc tren UI

1. Tai man review, bam `Reject`.
2. Nhap comment giai thich ly do tu choi.
3. Confirm.

### Ket qua mong doi

- Contract quay ve `DRAFT`
- Approval status chuyen sang `REJECTED`
- Accountant can vao sua hoac tao lai theo quy trinh noi bo

## 13. Buoc 10 - Test nhanh Request Modification

Tuong tu, co the tao 1 contract moi roi lap lai den buoc review.

### Cac buoc tren UI

1. Tai man review, bam `Request Modification`.
2. Nhap comment yeu cau accountant sua.
3. Confirm.

### Ket qua mong doi

- Contract quay ve `DRAFT`
- Approval status chuyen sang `MODIFICATION_REQUESTED`
- Accountant mo lai contract, sua voi `changeReason`, sau do submit lai

## 14. Buoc 11 - Kiem tra lai tren UI cua Accountant va Customer

### Tren UI Accountant

1. Dang nhap lai bang tai khoan accountant.
2. Mo Contract Detail.
3. Kiem tra:
   - status moi
   - approval status moi
   - status history
   - version history

### Tren UI Customer

1. Dang nhap lai bang tai khoan customer.
2. Mo Contract List.
3. Mo Contract Detail va Tracking.
4. Kiem tra customer thay trang thai hop dong da cap nhat.

## 15. Checklist tai hien flow thanh cong

Flow duoc xem la tai hien thanh cong neu dat du cac diem sau:

- Customer tao duoc quotation hop le
- Quotation vao `PENDING`
- Accountant nhin thay quotation va tao duoc contract
- Contract duoc tao o `DRAFT`
- Accountant submit contract va contract vao `PENDING_APPROVAL`
- Owner nhin thay contract trong Pending Approvals
- Owner co the Approve / Reject / Request Modification
- Trang thai contract va approval thay doi dung theo nhanh vua test

## 16. Loi thuong gap khi demo UI

- Khong tao duoc quotation: thuong la chua co bang gia active cho customer group
- Quotation tao duoc nhung khong du dieu kien submit: tong tien chua dat `10,000,000 VND`
- Accountant preview/create contract bi loi: thieu pricing hoac customer khong active
- Contract submit khong vao pending approval: tong tien chua vuot threshold va khong co rule approval nao duoc kich hoat
- Owner khong thay pending approval: contract thuc te da vao `SUBMITTED`, khong phai `PENDING_APPROVAL`

## 17. Ghi chu cho team UI/QA

- Neu UI chua hoan tat het man hinh, co the doi chieu voi Swagger de kiem tra backend status thuc te
- `Admin` trong UI can map dung sang role `OWNER`
- Guide nay uu tien tai hien flow approval. Neu chi muon test flow co ban khong can owner, co the tao contract co tong nho hon nguong approval
