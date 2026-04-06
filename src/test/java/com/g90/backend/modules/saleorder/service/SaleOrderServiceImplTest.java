// package com.g90.backend.modules.saleorder.service;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.when;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.g90.backend.dto.ValidationErrorItem;
// import com.g90.backend.exception.RequestValidationException;
// import com.g90.backend.exception.SaleOrderNotFoundException;
// import com.g90.backend.modules.account.entity.RoleName;
// import com.g90.backend.modules.account.repository.AuditLogRepository;
// import com.g90.backend.modules.contract.service.ContractService;
// import com.g90.backend.modules.contract.entity.ContractEntity;
// import com.g90.backend.modules.contract.entity.ContractItemEntity;
// import com.g90.backend.modules.contract.entity.ContractStatus;
// import com.g90.backend.modules.contract.integration.ContractInventoryGateway;
// import com.g90.backend.modules.contract.repository.ContractRepository;
// import com.g90.backend.modules.contract.repository.ContractStatusHistoryRepository;
// import com.g90.backend.modules.contract.repository.ContractTrackingEventRepository;
// import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
// import com.g90.backend.modules.inventory.repository.InventoryTransactionRepository;
// import com.g90.backend.modules.payment.repository.InvoiceRepository;
// import com.g90.backend.modules.payment.service.InvoiceService;
// import com.g90.backend.modules.product.entity.ProductEntity;
// import com.g90.backend.modules.project.repository.ProjectManagementRepository;
// import com.g90.backend.modules.saleorder.dto.SaleOrderStatusUpdateRequest;
// import com.g90.backend.modules.user.entity.CustomerProfileEntity;
// import com.g90.backend.modules.user.repository.CustomerProfileRepository;
// import com.g90.backend.security.AuthenticatedUser;
// import com.g90.backend.security.CurrentUserProvider;
// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.ZoneId;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.junit.jupiter.MockitoSettings;
// import org.mockito.quality.Strictness;

// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT)
// class SaleOrderServiceImplTest {

//     private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

//     @Mock
//     private ContractRepository contractRepository;
//     @Mock
//     private ContractStatusHistoryRepository contractStatusHistoryRepository;
//     @Mock
//     private ContractTrackingEventRepository contractTrackingEventRepository;
//     @Mock
//     private ProjectManagementRepository projectManagementRepository;
//     @Mock
//     private InventoryTransactionRepository inventoryTransactionRepository;
//     @Mock
//     private InvoiceRepository invoiceRepository;
//     @Mock
//     private PaymentAllocationRepository paymentAllocationRepository;
//     @Mock
//     private CustomerProfileRepository customerProfileRepository;
//     @Mock
//     private ContractInventoryGateway contractInventoryGateway;
//     @Mock
//     private ContractService contractService;
//     @Mock
//     private InvoiceService invoiceService;
//     @Mock
//     private AuditLogRepository auditLogRepository;
//     @Mock
//     private CurrentUserProvider currentUserProvider;

//     @InjectMocks
//     private SaleOrderServiceImpl saleOrderService;

//     @BeforeEach
//     void setUp() {
//         saleOrderService = new SaleOrderServiceImpl(
//                 contractRepository,
//                 contractStatusHistoryRepository,
//                 contractTrackingEventRepository,
//                 projectManagementRepository,
//                 inventoryTransactionRepository,
//                 invoiceRepository,
//                 paymentAllocationRepository,
//                 customerProfileRepository,
//                 contractInventoryGateway,
//                 contractService,
//                 invoiceService,
//                 auditLogRepository,
//                 currentUserProvider,
//                 new ObjectMapper().findAndRegisterModules()
//         );

//         when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
//         when(contractStatusHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
//         when(contractTrackingEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
//         when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
//         when(projectManagementRepository.findByLinkedContract_Id(any())).thenReturn(Optional.empty());
//         when(projectManagementRepository.findByLinkedContract_IdIn(any())).thenReturn(List.of());
//         when(inventoryTransactionRepository.findByRelatedOrderIdOrderByTransactionDateDescCreatedAtDesc(any())).thenReturn(List.of());
//         when(invoiceRepository.findByContractIdWithCustomerAndContract(any())).thenReturn(List.of());
//         when(paymentAllocationRepository.summarizeByInvoiceIds(any())).thenReturn(List.of());
//     }

//     @Test
//     void customerCannotAccessAnotherCustomersSaleOrder() {
//         authenticateAs(RoleName.CUSTOMER, "customer-user-1");
//         CustomerProfileEntity customer = customer("customer-1");

//         when(customerProfileRepository.findByUser_Id("customer-user-1")).thenReturn(Optional.of(customer));
//         when(contractRepository.findDetailedByIdAndCustomer_Id("contract-1", "customer-1")).thenReturn(Optional.empty());

//         assertThatThrownBy(() -> saleOrderService.getSaleOrder("contract-1"))
//                 .isInstanceOf(SaleOrderNotFoundException.class);
//     }

//     @Test
//     void registerInventoryIssueUpdatesIssuedQuantityWhenOrderIsPicked() {
//         ContractEntity saleOrder = saleOrder(ContractStatus.PICKED, "10.00", "0.00");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         saleOrderService.registerInventoryIssue("contract-1", "product-1", new BigDecimal("3.00"), "Batch A", "warehouse-1");

//         assertThat(saleOrder.getStatus()).isEqualTo(ContractStatus.PICKED.name());
//         assertThat(saleOrder.getItems().get(0).getIssuedQuantity()).isEqualByComparingTo("3.00");
//     }

//     @Test
//     void registerInventoryIssueCannotExceedOrderedQuantity() {
//         ContractEntity saleOrder = saleOrder(ContractStatus.PICKED, "10.00", "9.50");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         assertThatThrownBy(() -> saleOrderService.registerInventoryIssue("contract-1", "product-1", new BigDecimal("1.00"), null, "warehouse-1"))
//                 .isInstanceOf(RequestValidationException.class);
//     }

//     @Test
//     void registerInventoryIssueRejectedWhenOrderHasNotBeenPicked() {
//         ContractEntity saleOrder = saleOrder(ContractStatus.RESERVED, "10.00", "0.00");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         assertThatThrownBy(() -> saleOrderService.registerInventoryIssue("contract-1", "product-1", new BigDecimal("1.00"), null, "warehouse-1"))
//                 .isInstanceOfSatisfying(RequestValidationException.class, exception -> {
//                     assertThat(exception.getErrors())
//                             .extracting(ValidationErrorItem::getField, ValidationErrorItem::getMessage)
//                             .containsExactly(org.assertj.core.groups.Tuple.tuple(
//                                     "relatedOrderId",
//                                     "Inventory issue is not allowed for the current sale order status"
//                             ));
//                 });
//     }

//     @Test
//     void pickIsAllowedAfterReserveBeforeAnyInventoryIssue() {
//         authenticateAs(RoleName.WAREHOUSE, "warehouse-1");
//         ContractEntity saleOrder = saleOrder(ContractStatus.RESERVED, "10.00", "0.00");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         SaleOrderStatusUpdateRequest request = new SaleOrderStatusUpdateRequest();
//         request.setStatus("PICKED");

//         var response = saleOrderService.updateStatus("contract-1", request);

//         assertThat(response.currentStatus()).isEqualTo(ContractStatus.PICKED.name());
//         assertThat(saleOrder.getStatus()).isEqualTo(ContractStatus.PICKED.name());
//     }

//     @Test
//     void dispatchRequiresAllItemsToBeIssuedAfterPick() {
//         authenticateAs(RoleName.WAREHOUSE, "warehouse-1");
//         ContractEntity saleOrder = saleOrder(ContractStatus.PICKED, "10.00", "3.00");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         SaleOrderStatusUpdateRequest request = new SaleOrderStatusUpdateRequest();
//         request.setStatus("IN_TRANSIT");

//         assertThatThrownBy(() -> saleOrderService.updateStatus("contract-1", request))
//                 .isInstanceOfSatisfying(RequestValidationException.class, exception -> {
//                     assertThat(exception.getErrors())
//                             .extracting(ValidationErrorItem::getField, ValidationErrorItem::getMessage)
//                             .containsExactly(org.assertj.core.groups.Tuple.tuple(
//                                     "status",
//                                     "All sale order items must be fully issued before continuing"
//                             ));
//                 });
//     }

//     @Test
//     void deliveredMustComeFromInTransit() {
//         authenticateAs(RoleName.WAREHOUSE, "warehouse-1");
//         ContractEntity saleOrder = saleOrder(ContractStatus.PICKED, "10.00", "10.00");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         SaleOrderStatusUpdateRequest request = new SaleOrderStatusUpdateRequest();
//         request.setStatus("DELIVERED");
//         request.setActualDeliveryDate(LocalDate.of(2026, 4, 6));

//         assertThatThrownBy(() -> saleOrderService.updateStatus("contract-1", request))
//                 .isInstanceOfSatisfying(RequestValidationException.class, exception -> {
//                     assertThat(exception.getErrors())
//                             .extracting(ValidationErrorItem::getField, ValidationErrorItem::getMessage)
//                             .containsExactly(org.assertj.core.groups.Tuple.tuple(
//                                     "status",
//                                     "Invalid sale order status transition"
//                             ));
//                 });
//     }

//     @Test
//     void cancelledOrderCannotContinueFulfillment() {
//         authenticateAs(RoleName.WAREHOUSE, "warehouse-1");
//         ContractEntity saleOrder = saleOrder(ContractStatus.CANCELLED, "10.00", "10.00");
//         when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(saleOrder));

//         SaleOrderStatusUpdateRequest request = new SaleOrderStatusUpdateRequest();
//         request.setStatus("PROCESSING");

//         assertThatThrownBy(() -> saleOrderService.updateStatus("contract-1", request))
//                 .isInstanceOf(RequestValidationException.class);
//     }

//     private void authenticateAs(RoleName roleName, String userId) {
//         when(currentUserProvider.getCurrentUser()).thenReturn(
//                 new AuthenticatedUser(userId, userId + "@g90.vn", roleName.name(), "token")
//         );
//     }

//     private CustomerProfileEntity customer(String customerId) {
//         CustomerProfileEntity customer = new CustomerProfileEntity();
//         customer.setId(customerId);
//         return customer;
//     }

//     private ContractEntity saleOrder(ContractStatus status, String orderedQuantity, String issuedQuantity) {
//         ProductEntity product = new ProductEntity();
//         product.setId("product-1");
//         product.setProductCode("SP001");
//         product.setProductName("Steel Coil");
//         product.setUnit("KG");

//         ContractItemEntity item = new ContractItemEntity();
//         item.setId("item-1");
//         item.setProduct(product);
//         item.setQuantity(new BigDecimal(orderedQuantity));
//         item.setReservedQuantity(new BigDecimal(orderedQuantity));
//         item.setIssuedQuantity(new BigDecimal(issuedQuantity));
//         item.setDeliveredQuantity(BigDecimal.ZERO.setScale(2));
//         item.setUnitPrice(new BigDecimal("100.00"));
//         item.setTotalPrice(new BigDecimal("1000.00"));

//         CustomerProfileEntity customer = customer("customer-1");
//         customer.setCompanyName("ABC Steel");

//         ContractEntity contract = new ContractEntity();
//         contract.setId("contract-1");
//         contract.setContractNumber("CT-001");
//         contract.setSaleOrderNumber("SO-20260405-0001");
//         contract.setStatus(status.name());
//         contract.setCustomer(customer);
//         contract.setSubmittedAt(LocalDateTime.now(APP_ZONE).minusDays(1));
//         contract.setCreatedAt(LocalDateTime.now(APP_ZONE).minusDays(2));
//         contract.setUpdatedAt(LocalDateTime.now(APP_ZONE));
//         contract.setItems(List.of(item));
//         item.setContract(contract);
//         return contract;
//     }
// }
