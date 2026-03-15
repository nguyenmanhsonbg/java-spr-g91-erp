# Flow chi tiết: Customer tạo Quotation → Accountant tạo Contract → Owner/Admin phê duyệt Contract

## 1. Mục đích tài liệu
Tài liệu này mô tả chi tiết luồng màn hình theo SRS cho flow:

1. Customer tạo quotation
2. Accountant tiếp nhận quotation và tạo contract
3. Owner/Admin phê duyệt contract

> Ghi chú: theo SRS, vai trò phê duyệt hợp đồng là **Owner**. Nếu team đang dùng từ **Admin** trong UI hoặc nội bộ, nên map đúng với quyền **Owner** để tránh lệch đặc tả.

---

## 2. Phạm vi flow
Flow này bao gồm các nhóm màn hình chính:

- Customer side
  - Login
  - Product Catalog / Product Detail
  - Quotation Request
  - Quotation Preview
  - Quotation List
  - Quotation Detail

- Accountant side
  - Login
  - Quotation List
  - Quotation Detail / Review
  - Create Contract
  - Contract Detail
  - Update Contract

- Owner side
  - Login
  - Approve Contract / Pending Approvals
  - Contract Review
  - Approve / Reject / Request Modification

---

## 3. Tổng quan flow nghiệp vụ

### Luồng tổng quát
1. Customer đăng nhập hệ thống
2. Customer xem sản phẩm và tạo quotation request
3. Customer submit quotation
4. Hệ thống lưu quotation và thông báo cho Accountant
5. Accountant xem quotation và review nội dung
6. Accountant tạo contract từ quotation hoặc nhập mới dựa trên quotation
7. Contract được lưu ở trạng thái Draft / Pending Approval tùy rule
8. Nếu contract thuộc diện cần phê duyệt, Owner vào màn phê duyệt
9. Owner xem chi tiết hợp đồng và ra quyết định Approve / Reject / Request Modification
10. Hệ thống cập nhật trạng thái và gửi thông báo lại cho Accountant và Customer

---

## 4. Flow chi tiết theo màn hình

# PHẦN A - CUSTOMER TẠO QUOTATION

## Screen A1. Login
**Actor:** Customer  
**Mục đích:** Đăng nhập để sử dụng các chức năng quotation theo quyền được cấp.

### Thành phần chính
- Username / Email
- Password
- Nút Login
- Link quên mật khẩu (nếu có)

### Hành động chính
- Customer nhập thông tin đăng nhập
- Hệ thống xác thực thành công
- Điều hướng về màn hình chính hoặc khu vực quotation/product

### Kết quả
- Customer được quyền truy cập các màn:
  - Quotation Request
  - Quotation List
  - các màn xem sản phẩm

---

## Screen A2. Product Catalog / Product List
**Actor:** Customer  
**Mục đích:** Xem danh sách sản phẩm trước khi tạo quotation.

### Thành phần chính
- Danh sách sản phẩm
- Tìm kiếm theo tên/mã sản phẩm
- Bộ lọc theo loại / nhóm sản phẩm
- Giá tham khảo hoặc thông tin cơ bản
- Nút xem chi tiết sản phẩm
- Nút thêm vào quotation / yêu cầu báo giá (nếu UI hỗ trợ)

### Hành động chính
- Customer tìm kiếm sản phẩm quan tâm
- Chọn sản phẩm muốn yêu cầu báo giá
- Đi tới màn Product Detail hoặc Quotation Request

### Kết quả
- Customer xác định được danh sách mặt hàng cần báo giá

---

## Screen A3. Product Detail
**Actor:** Customer  
**Mục đích:** Xem chi tiết sản phẩm trước khi thêm vào quotation.

### Thành phần chính
- Mã sản phẩm
- Tên sản phẩm
- Quy cách / thông số kỹ thuật
- Đơn vị tính
- Mô tả
- Hình ảnh (nếu có)
- Nút thêm vào quotation

### Hành động chính
- Customer xem thông tin kỹ thuật sản phẩm
- Xác nhận chọn sản phẩm để đưa vào quotation request

### Kết quả
- Điều hướng sang màn Quotation Request với sản phẩm đã chọn sẵn hoặc Customer tự thêm ở màn quotation

---

## Screen A4. Quotation Request
**Actor:** Customer  
**Mục đích:** Tạo một quotation request gửi cho hệ thống.

### Thành phần chính
- Thông tin khách hàng (có thể auto-fill)
- Danh sách sản phẩm yêu cầu báo giá
  - Product
  - Quantity
  - Unit
- Project reference
- Delivery requirements
- Ghi chú thêm
- Mã khuyến mãi / promotion code (nếu có)
- Tổng tiền tạm tính / quotation preview
- Nút:
  - Save as Draft
  - Apply Promotion
  - Submit

### Hành động chính
1. Customer chọn hoặc thêm sản phẩm
2. Nhập số lượng cho từng sản phẩm
3. Nhập `project reference`
4. Nhập `delivery requirements`
5. Nhập ghi chú bổ sung (nếu có)
6. Hệ thống tính toán giá theo customer-specific pricing
7. Customer có thể:
   - Save as Draft
   - Apply Promotion
   - Submit quotation

### Rule cần thể hiện trên UI/BE
- Chỉ Customer mới tạo quotation của chính mình
- Hệ thống tính giá theo nhóm giá của customer
- Có thể lưu nháp trước khi submit
- Khi submit, quotation chuyển sang trạng thái phù hợp để Accountant xử lý

### Kết quả
- Nếu Save as Draft: quotation được lưu nháp
- Nếu Submit: quotation được lưu và chuyển trạng thái chờ xử lý
- Hệ thống gửi thông báo cho Accountant phụ trách

---

## Screen A5. Quotation Preview
**Actor:** Customer  
**Mục đích:** Kiểm tra lại nội dung quotation trước khi gửi.

### Thành phần chính
- Thông tin sản phẩm đã chọn
- Số lượng
- Giá tạm tính / giá theo pricing rule
- Tổng tiền
- Project reference
- Delivery requirements
- Ghi chú
- Nút Back / Edit
- Nút Confirm Submit

### Hành động chính
- Customer rà soát lại toàn bộ nội dung quotation
- Quay lại sửa nếu cần
- Xác nhận gửi quotation

### Kết quả
- Quotation được gửi chính thức

---

## Screen A6. Quotation List
**Actor:** Customer  
**Mục đích:** Xem danh sách quotation của chính mình.

### Thành phần chính
- Bảng danh sách quotation
- Các cột gợi ý:
  - Quotation Number
  - Created Date
  - Total Amount
  - Status
- Bộ lọc theo trạng thái
- Bộ lọc theo ngày
- Ô tìm kiếm
- Nút xem chi tiết quotation

### Hành động chính
- Customer xem danh sách quotation đã tạo
- Lọc theo trạng thái hoặc khoảng ngày
- Chọn 1 quotation để mở chi tiết

### Rule
- Customer chỉ xem được quotation của chính mình

### Kết quả
- Customer theo dõi được các quotation đã gửi

---

## Screen A7. Quotation Detail
**Actor:** Customer  
**Mục đích:** Xem chi tiết một quotation đã tạo.

### Thành phần chính
- Thông tin quotation
- Danh sách item
- Số lượng / đơn giá / thành tiền
- Project reference
- Delivery requirements
- Trạng thái quotation
- Lịch sử xử lý cơ bản (nếu có)

### Hành động chính
- Customer theo dõi tình trạng quotation
- Kiểm tra lại nội dung đã gửi

### Kết quả
- Customer nắm được quotation đang ở trạng thái nào để chờ Accountant xử lý tiếp

---

# PHẦN B - ACCOUNTANT TIẾP NHẬN QUOTATION VÀ TẠO CONTRACT

## Screen B1. Login
**Actor:** Accountant  
**Mục đích:** Đăng nhập hệ thống để xử lý quotation và contract.

### Kết quả
- Accountant vào các màn quản lý quotation/contract theo role

---

## Screen B2. Quotation List
**Actor:** Accountant  
**Mục đích:** Xem tất cả quotation để xử lý.

### Thành phần chính
- Danh sách quotation toàn hệ thống
- Bộ lọc:
  - Status
  - Date Range
  - Customer
- Ô tìm kiếm theo mã quotation
- Nút mở chi tiết quotation

### Hành động chính
- Accountant lọc các quotation cần xử lý
- Chọn quotation của một customer cụ thể

### Rule
- Accountant được xem tất cả quotation

### Kết quả
- Chuyển sang màn chi tiết quotation để review

---

## Screen B3. Quotation Detail / Review Quotation
**Actor:** Accountant  
**Mục đích:** Review quotation trước khi tạo contract.

### Thành phần chính
- Thông tin customer
- Danh sách sản phẩm khách yêu cầu
- Số lượng
- Giá tham chiếu / giá hiện hành
- Project reference
- Delivery requirements
- Ghi chú của customer
- Trạng thái quotation
- Nút:
  - Create Contract
  - Copy to Contract
  - Back

### Hành động chính
- Accountant kiểm tra nội dung quotation
- Xác minh tính phù hợp về giá, sản phẩm, số lượng, delivery requirements
- Bắt đầu quy trình tạo contract từ quotation

### Kết quả
- Điều hướng sang màn Create Contract

---

## Screen B4. Create Contract
**Actor:** Accountant  
**Mục đích:** Tạo hợp đồng cho customer dựa trên quotation hoặc tạo mới.

### Thành phần chính
- Contract Number (auto-generate)
- Customer selector / customer info
- Customer credit limit
- Current debt
- Danh sách sản phẩm
  - Product
  - Quantity
  - Unit price
  - Amount
- Delivery terms
- Payment terms
- Delivery address
- Ghi chú nội bộ
- Nút:
  - Save Draft
  - Copy from Quotation
  - Preview Contract
  - Submit for Approval (nếu áp dụng)

### Hành động chính
1. Accountant chọn customer hoặc load sẵn từ quotation
2. Hệ thống hiển thị `credit limit` và `current debt`
3. Accountant thêm/chỉnh sản phẩm, số lượng, giá thương lượng trong biên độ cho phép
4. Nhập `delivery terms`
5. Nhập `payment terms`
6. Lưu contract ở trạng thái Draft
7. Nếu contract cần approval thì đưa vào luồng chờ duyệt

### Rule
- Hệ thống tự sinh contract number
- Có thể copy dữ liệu từ quotation
- Hợp đồng giá trị lớn có thể cần Owner approval
- Dữ liệu customer credit/debt nên được hiển thị trước khi xác nhận

### Kết quả
- Contract được tạo thành công ở trạng thái Draft hoặc Pending Approval tùy rule

---

## Screen B5. Contract Preview
**Actor:** Accountant  
**Mục đích:** Kiểm tra toàn bộ nội dung hợp đồng trước khi lưu/chuyển duyệt.

### Thành phần chính
- Contract number
- Customer info
- Item list
- Tổng tiền
- Delivery terms
- Payment terms
- Điều khoản chính
- Nút Edit
- Nút Save Draft
- Nút Submit for Approval

### Hành động chính
- Accountant rà soát lại nội dung hợp đồng
- Sửa lại nếu cần
- Xác nhận gửi duyệt nếu hợp đồng cần phê duyệt

### Kết quả
- Contract sẵn sàng cho bước review / approval

---

## Screen B6. Contract Detail
**Actor:** Accountant  
**Mục đích:** Xem chi tiết hợp đồng đã tạo.

### Thành phần chính
- Customer info
- Danh sách item và quy cách
- Unit prices
- Quantities
- Total amount
- Terms and conditions
- Delivery address
- Payment schedule
- Version history / change history
- Status của contract
- Nút Update Contract
- Nút Submit / Resubmit for Approval (nếu phù hợp)

### Hành động chính
- Accountant kiểm tra trạng thái hợp đồng
- Mở chỉnh sửa nếu hợp đồng còn cho phép sửa
- Theo dõi lịch sử thay đổi

### Kết quả
- Accountant chuẩn bị dữ liệu hoàn chỉnh trước khi Owner duyệt

---

## Screen B7. Update Contract
**Actor:** Accountant  
**Mục đích:** Chỉnh sửa contract trước khi được finalized.

### Thành phần chính
- Toàn bộ form contract có thể chỉnh sửa
- Trường bắt buộc nhập lý do thay đổi
- Hiển thị version hiện tại
- Hiển thị diff hoặc thông tin thay đổi (nếu có)
- Nút Save
- Nút Resubmit for Approval

### Hành động chính
- Accountant cập nhật item / quantity / price / delivery info / notes
- Nhập lý do thay đổi
- Hệ thống lưu version mới
- Nếu thay đổi vượt ngưỡng rule thì đi lại approval path

### Rule
- Chỉ contract ở trạng thái phù hợp mới cho phép sửa
- Price change lớn hơn ngưỡng quy định cần approval
- Mọi thay đổi phải audit log

### Kết quả
- Contract được cập nhật và sẵn sàng chờ duyệt

---

# PHẦN C - OWNER / ADMIN PHÊ DUYỆT CONTRACT

## Screen C1. Login
**Actor:** Owner  
**Mục đích:** Đăng nhập để xử lý contract cần phê duyệt.

### Kết quả
- Owner truy cập được màn hình Approve Contract

---

## Screen C2. Pending Approvals / Approve Contract List
**Actor:** Owner  
**Mục đích:** Xem danh sách contract đang chờ phê duyệt.

### Thành phần chính
- Danh sách contract pending approval
- Các cột gợi ý:
  - Contract Number
  - Customer
  - Total Amount
  - Requested By
  - Submitted Date
  - Approval Status
- Bộ lọc:
  - date range
  - customer
  - amount range
  - status
- Nút xem chi tiết contract

### Hành động chính
- Owner xem các hợp đồng chờ duyệt
- Chọn một contract cụ thể để review

### Kết quả
- Điều hướng sang màn Contract Review for Approval

---

## Screen C3. Contract Review for Approval
**Actor:** Owner  
**Mục đích:** Review chi tiết contract trước khi đưa ra quyết định.

### Thành phần chính
- Contract details đầy đủ
- Customer history
- Profit margin
- Credit risk / debt snapshot
- Delivery terms
- Payment terms
- Change history / version history
- Lý do cần approval
- Nút:
  - Approve
  - Reject
  - Request Modification

### Hành động chính
- Owner xem toàn bộ nội dung hợp đồng
- Đánh giá rủi ro tài chính / biên lợi nhuận / lịch sử khách hàng
- Chọn 1 trong 3 hành động:
  - Approve
  - Reject
  - Request Modification

### Rule
- Các hợp đồng lớn hơn ngưỡng quy định bắt buộc phải đi qua bước này
- Chỉ Owner mới có quyền quyết định approval

### Kết quả
- Contract chuyển sang trạng thái tương ứng sau review

---

## Screen C4. Approval Action Dialog / Decision Form
**Actor:** Owner  
**Mục đích:** Xác nhận quyết định phê duyệt.

### Thành phần chính
- Action type: Approve / Reject / Request Modification
- Comment / Note
- Optional reason when reject/request modification
- Confirm button
- Cancel button

### Hành động chính
- Owner nhập ghi chú quyết định
- Xác nhận hành động

### Kết quả
- Hệ thống cập nhật approval status
- Gửi thông báo cho Accountant và Customer

---

# PHẦN D - KẾT QUẢ SAU PHÊ DUYỆT

## Trường hợp 1: Approve
- Contract được chuyển sang trạng thái đã duyệt
- Accountant nhận thông báo để tiếp tục các bước downstream
- Customer nhận thông báo rằng hợp đồng đã được chấp nhận / phê duyệt

## Trường hợp 2: Reject
- Contract bị từ chối
- Accountant nhận lý do từ chối
- Có thể cần tạo lại hoặc chỉnh sửa theo quy trình nội bộ
- Customer có thể được thông báo tùy rule nghiệp vụ

## Trường hợp 3: Request Modification
- Contract quay lại cho Accountant chỉnh sửa
- Accountant cập nhật và resubmit
- Flow quay lại bước review của Owner

---

## 5. Sơ đồ điều hướng màn hình (text flow)

### Customer flow
Login  
→ Product Catalog  
→ Product Detail  
→ Quotation Request  
→ Quotation Preview  
→ Submit  
→ Quotation List  
→ Quotation Detail

### Accountant flow
Login  
→ Quotation List  
→ Quotation Detail / Review  
→ Create Contract  
→ Contract Preview  
→ Contract Detail  
→ Update Contract (nếu cần)  
→ Submit for Approval

### Owner flow
Login  
→ Pending Approvals  
→ Contract Review  
→ Approve / Reject / Request Modification

---

## 6. Trạng thái chính trong flow

### Quotation
- Draft
- Pending
- Approved / Rejected / Converted (tùy triển khai sâu hơn)

### Contract
- Draft
- Pending Approval
- Approved
- Rejected
- Request Modification / Returned

> Ghi chú: Các trạng thái sâu hơn như Submitted / Processing / Delivered / Completed thuộc flow tiếp theo sau approval, không phải trọng tâm của tài liệu này.

---

## 7. Rule quan trọng UI/BE cần chú ý

### Đối với Customer
- Chỉ tạo quotation cho chính mình
- Chỉ xem quotation của chính mình
- Không mặc định cho quyền quản lý quotation của người khác

### Đối với Accountant
- Xem tất cả quotation
- Tạo contract từ quotation
- Kiểm tra customer credit limit và current debt trước khi xác nhận contract
- Theo dõi lịch sử chỉnh sửa contract

### Đối với Owner
- Chỉ Owner có quyền approve contract theo flow SRS
- Các hợp đồng vượt ngưỡng giá trị phải đi qua approval

---

## 8. Điểm lệch/không đồng nhất trong SRS cần lưu ý

### Lệch 1: Create Quotation bị ghi nhầm tên use case
- Heading thể hiện đây là luồng **Create Quotation**
- Nhưng phần bên trong lại ghi nhầm **Use Case Name: Create Contract**
- Khi làm UI/BE, phải bám theo heading và flow nghiệp vụ thực tế của customer

### Lệch 2: Vai trò phê duyệt là Owner, không phải Admin
- Nếu team nội bộ đang dùng từ “Admin”, cần làm rõ có phải đang map sang role Owner hay không
- Trong đặc tả hiện tại nên coi **Owner** là người phê duyệt chuẩn

### Lệch 3: View Contract của Customer có thể cần chốt lại
- Một số phần trong tài liệu cho thấy Customer có thể xem contract của chính mình
- Nhưng screen authorization có chỗ thể hiện chưa hoàn toàn đồng nhất
- Khi chốt UI/BE nên thống nhất rule cuối cùng với BA/PO

---

## 9. Gợi ý tách UI screen list cho UI Dev

1. Login
2. Product Catalog
3. Product Detail
4. Quotation Request
5. Quotation Preview
6. Quotation List (Customer)
7. Quotation Detail
8. Quotation List (Accountant)
9. Quotation Review
10. Create Contract
11. Contract Preview
12. Contract Detail
13. Update Contract
14. Pending Approval List
15. Contract Approval Review
16. Approval Action Dialog

---

## 10. Kết luận
Flow chính theo SRS có thể hiểu gọn như sau:

- **Customer** tạo quotation từ nhu cầu mua hàng / dự án
- **Accountant** tiếp nhận quotation và chuyển thành contract với đầy đủ điều khoản thương mại
- **Owner** thực hiện phê duyệt các contract cần kiểm soát trước khi bước vào giai đoạn xử lý tiếp theo

Tài liệu này nên được dùng làm nền cho:
- thiết kế UI screen flow
- đặc tả API
- phân quyền
- status transition
- test case cho luồng quotation → contract → approval
