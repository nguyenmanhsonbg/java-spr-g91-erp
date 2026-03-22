package com.g90.backend.modules.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ContractCreditLimitExceededException;
import com.g90.backend.exception.ContractInventoryUnavailableException;
import com.g90.backend.exception.ContractNotEditableException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.dto.ContractApprovalDecisionRequest;
import com.g90.backend.modules.contract.dto.ContractFormInitQuery;
import com.g90.backend.modules.contract.dto.ContractCreateRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentEmailRequest;
import com.g90.backend.modules.contract.dto.ContractItemRequest;
import com.g90.backend.modules.contract.dto.PendingContractApprovalListQuery;
import com.g90.backend.modules.contract.dto.ContractSubmitRequest;
import com.g90.backend.modules.contract.dto.ContractUpdateRequest;
import com.g90.backend.modules.contract.dto.CreateContractFromQuotationRequest;
import com.g90.backend.modules.contract.entity.ContractApprovalEntity;
import com.g90.backend.modules.contract.entity.ContractApprovalStatus;
import com.g90.backend.modules.contract.entity.ContractDocumentEntity;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.entity.ContractPendingAction;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.contract.integration.ContractCreditGateway;
import com.g90.backend.modules.contract.integration.ContractDocumentGateway;
import com.g90.backend.modules.contract.integration.ContractEmailGateway;
import com.g90.backend.modules.contract.integration.ContractInventoryGateway;
import com.g90.backend.modules.contract.integration.ContractNotificationGateway;
import com.g90.backend.modules.contract.integration.ContractPricingGateway;
import com.g90.backend.modules.contract.integration.ContractSchedulerSupport;
import com.g90.backend.modules.contract.mapper.ContractMapper;
import com.g90.backend.modules.contract.repository.ContractApprovalRepository;
import com.g90.backend.modules.contract.repository.ContractDocumentRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.contract.repository.ContractStatusHistoryRepository;
import com.g90.backend.modules.contract.repository.ContractTrackingEventRepository;
import com.g90.backend.modules.contract.repository.ContractVersionRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.project.repository.ProjectInvoiceRepository;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import com.g90.backend.modules.quotation.entity.QuotationStatus;
import com.g90.backend.modules.quotation.repository.QuotationRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceImplTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractVersionRepository contractVersionRepository;
    @Mock
    private ContractApprovalRepository contractApprovalRepository;
    @Mock
    private ContractStatusHistoryRepository contractStatusHistoryRepository;
    @Mock
    private ContractDocumentRepository contractDocumentRepository;
    @Mock
    private ContractTrackingEventRepository contractTrackingEventRepository;
    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private ProjectInvoiceRepository projectInvoiceRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ContractPricingGateway contractPricingGateway;
    @Mock
    private ContractCreditGateway contractCreditGateway;
    @Mock
    private ContractInventoryGateway contractInventoryGateway;
    @Mock
    private ContractNotificationGateway contractNotificationGateway;
    @Mock
    private ContractDocumentGateway contractDocumentGateway;
    @Mock
    private ContractEmailGateway contractEmailGateway;
    @Mock
    private ContractSchedulerSupport contractSchedulerSupport;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final ContractMapper contractMapper = new ContractMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private ContractServiceImpl contractService;

    @BeforeEach
    void setUp() {
        contractService = new ContractServiceImpl(
                contractRepository,
                contractVersionRepository,
                contractApprovalRepository,
                contractStatusHistoryRepository,
                contractDocumentRepository,
                contractTrackingEventRepository,
                quotationRepository,
                productRepository,
                customerProfileRepository,
                projectInvoiceRepository,
                auditLogRepository,
                contractPricingGateway,
                contractCreditGateway,
                contractInventoryGateway,
                contractNotificationGateway,
                contractDocumentGateway,
                contractEmailGateway,
                contractSchedulerSupport,
                contractMapper,
                currentUserProvider,
                objectMapper
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractApprovalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractStatusHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractTrackingEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractRepository.save(any())).thenAnswer(invocation -> {
            ContractEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("contract-1");
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now(APP_ZONE));
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            return entity;
        });
        when(contractSchedulerSupport.nextAutoSubmitAt(any())).thenAnswer(invocation -> ((LocalDateTime) invocation.getArgument(0)).plusDays(7));
        when(contractSchedulerSupport.approvalDueAt(any())).thenAnswer(invocation -> ((LocalDateTime) invocation.getArgument(0)).plusHours(24));
        when(projectInvoiceRepository.findByContractId(anyString())).thenReturn(List.of());
    }

    @Test
    void createContractSuccess() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractCreateRequest request = createRequest();

        when(customerProfileRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of("product-1"))).thenReturn(List.of(product));
        when(contractPricingGateway.resolveBasePrices(any(), anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractPricingGateway.PricingData("product-1", new BigDecimal("1000.00"), "pl-1", "Price List")
        ));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("1000000.00"), false)
        );
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", new BigDecimal("500.00"), true)
        ));
        when(contractRepository.findDetailedById("contract-1")).thenAnswer(invocation -> Optional.of(invocation.getMock() == null ? null : savedContract("contract-1", customer, product)));

        var response = contractService.createContract(request);

        assertThat(response.contract().status()).isEqualTo(ContractStatus.DRAFT.name());
        assertThat(response.contract().totalAmount()).isEqualByComparingTo("9000.00");
        assertThat(response.items()).hasSize(1);
        verify(contractNotificationGateway).notifyContractCreated(any(), anyString());
    }

    @Test
    void getContractFormInitFromQuotationSuccess() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        QuotationEntity quotation = quotation("quotation-1", customer, product);

        when(quotationRepository.findDetailedById("quotation-1")).thenReturn(Optional.of(quotation));
        when(customerProfileRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(contractPricingGateway.resolveBasePrices(any(), anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractPricingGateway.PricingData("product-1", new BigDecimal("1000.00"), "pl-1", "Price List")
        ));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000.00"), new BigDecimal("10000.00"), new BigDecimal("990000.00"), false)
        );

        ContractFormInitQuery query = new ContractFormInitQuery();
        query.setQuotationId("quotation-1");

        var response = contractService.getContractFormInit(query);

        assertThat(response.customer().id()).isEqualTo("customer-1");
        assertThat(response.quotation().quotationNumber()).isEqualTo("QT-20260315-0001");
        assertThat(response.defaults().suggestedPaymentTerms()).isEqualTo("70% on delivery, 30% within 30 days");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).baseUnitPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void createContractFromQuotationSuccess() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        QuotationEntity quotation = quotation("quotation-1", customer, product);

        when(quotationRepository.findDetailedById("quotation-1")).thenReturn(Optional.of(quotation));
        when(customerProfileRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of("product-1"))).thenReturn(List.of(product));
        when(contractPricingGateway.resolveBasePrices(any(), anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractPricingGateway.PricingData("product-1", new BigDecimal("1000.00"), "pl-1", "Price List")
        ));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("1000000.00"), false)
        );
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", new BigDecimal("500.00"), true)
        ));
        when(contractRepository.existsByQuotation_Id("quotation-1")).thenReturn(false);
        ContractEntity savedContract = savedContract("contract-1", customer, product);
        savedContract.setQuotation(quotation);
        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(savedContract));

        CreateContractFromQuotationRequest request = new CreateContractFromQuotationRequest();
        request.setPaymentTerms("70% on delivery, 30% within 30 days");
        request.setDeliveryAddress("Warehouse district 9");

        var response = contractService.createFromQuotation("quotation-1", request);

        assertThat(response.contract().quotationId()).isEqualTo("quotation-1");
        assertThat(response.quotation().status()).isEqualTo(QuotationStatus.CONVERTED.name());
        verify(quotationRepository).save(any());
    }

    @Test
    void createBlockedWhenCreditLimitExceeded() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        customer.setCreditLimit(new BigDecimal("100.00"));
        ProductEntity product = product("product-1", "SP001");

        when(customerProfileRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of("product-1"))).thenReturn(List.of(product));
        when(contractPricingGateway.resolveBasePrices(any(), anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractPricingGateway.PricingData("product-1", new BigDecimal("1000.00"), "pl-1", "Price List")
        ));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("100.00"), new BigDecimal("95.00"), new BigDecimal("5.00"), false)
        );
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", new BigDecimal("500.00"), true)
        ));

        assertThatThrownBy(() -> contractService.createContract(createRequest()))
                .isInstanceOf(ContractCreditLimitExceededException.class);
    }

    @Test
    void customerCanViewOwnContract() {
        authenticateAs(RoleName.CUSTOMER, "user-customer-1");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);

        when(customerProfileRepository.findByUser_Id("user-customer-1")).thenReturn(Optional.of(customer));
        when(contractRepository.findDetailedByIdAndCustomer_Id("contract-1", "customer-1")).thenReturn(Optional.of(contract));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("1000000.00"), false)
        );

        var response = contractService.getContractDetail("contract-1");

        assertThat(response.contract().id()).isEqualTo("contract-1");
        assertThat(response.contract().customerId()).isEqualTo("customer-1");
    }

    @Test
    void customerCannotViewAnotherContract() {
        authenticateAs(RoleName.CUSTOMER, "user-customer-1");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");

        when(customerProfileRepository.findByUser_Id("user-customer-1")).thenReturn(Optional.of(customer));
        when(contractRepository.findDetailedByIdAndCustomer_Id("contract-1", "customer-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.getContractDetail("contract-1"))
                .isInstanceOf(com.g90.backend.exception.ContractNotFoundException.class);
    }

    @Test
    void updateNonDraftBlocked() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        contract.setStatus(ContractStatus.SUBMITTED.name());

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));

        ContractUpdateRequest request = new ContractUpdateRequest();
        request.setCustomerId("customer-1");
        request.setPaymentTerms("terms");
        request.setDeliveryAddress("address");
        request.setItems(List.of(itemRequest("product-1", "9.00", "1000.00")));
        request.setChangeReason("Need update");

        assertThatThrownBy(() -> contractService.updateContract("contract-1", request))
                .isInstanceOf(ContractNotEditableException.class);
    }

    @Test
    void submitSuccess() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("1000000.00"), false)
        );
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", new BigDecimal("500.00"), true)
        ));

        var response = contractService.submitContract("contract-1", new ContractSubmitRequest());

        assertThat(response.contractStatus()).isEqualTo(ContractStatus.SUBMITTED.name());
        verify(contractInventoryGateway).reserveInventory(any());
    }

    @Test
    void highValueContractEntersApprovalFlow() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        contract.setTotalAmount(new BigDecimal("600000000.00"));
        contract.setRequiresApproval(true);

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000000.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("1000000000.00"), false)
        );
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", new BigDecimal("500.00"), true)
        ));

        var response = contractService.submitContract("contract-1", new ContractSubmitRequest());

        assertThat(response.decision()).isEqualTo("PENDING_APPROVAL");
        assertThat(response.contractStatus()).isEqualTo(ContractStatus.PENDING_APPROVAL.name());
    }

    @Test
    void submitBlockedWhenInventoryInsufficient() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("1000000.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("1000000.00"), false)
        );
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", BigDecimal.ZERO.setScale(2), false)
        ));

        assertThatThrownBy(() -> contractService.submitContract("contract-1", new ContractSubmitRequest()))
                .isInstanceOf(ContractInventoryUnavailableException.class);
    }

    @Test
    void approveContractByOwnerSuccess() {
        authenticateAs(RoleName.OWNER, "user-owner");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        contract.setStatus(ContractStatus.PENDING_APPROVAL.name());
        contract.setApprovalStatus(ContractApprovalStatus.PENDING.name());
        contract.setPendingAction(ContractPendingAction.SUBMIT.name());
        ContractApprovalEntity approval = new ContractApprovalEntity();
        approval.setId("approval-1");
        approval.setContract(contract);
        approval.setStatus(ContractApprovalStatus.PENDING.name());
        approval.setApprovalType("SUBMISSION");

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(contractApprovalRepository.findByContract_IdOrderByRequestedAtDesc("contract-1")).thenReturn(List.of(approval));
        when(contractInventoryGateway.checkAvailability(anyCollection())).thenReturn(Map.of(
                "product-1",
                new ContractInventoryGateway.InventoryAvailability("product-1", new BigDecimal("500.00"), true)
        ));

        var response = contractService.approveContract("contract-1", new ContractApprovalDecisionRequest());

        assertThat(response.decision()).isEqualTo("APPROVED");
        assertThat(response.contractStatus()).isEqualTo(ContractStatus.SUBMITTED.name());
        verify(contractNotificationGateway).notifyContractApproved(any(), anyString());
    }

    @Test
    void emailDocumentUsesEmailGatewayAndUpdatesTimestamp() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        ContractDocumentEntity document = new ContractDocumentEntity();
        document.setId("document-1");
        document.setContract(contract);
        document.setDocumentType("SALES_CONTRACT");
        document.setFileName("SC-CT-20260315-0001.pdf");
        contract.setDocuments(new ArrayList<>(List.of(document)));

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(contractEmailGateway.sendDocument(document, "recipient@example.com"))
                .thenReturn(new ContractEmailGateway.EmailResult(true, "queued"));

        ContractDocumentEmailRequest request = new ContractDocumentEmailRequest();
        request.setRecipientEmail("recipient@example.com");

        var response = contractService.emailDocument("contract-1", "document-1", request);

        assertThat(response.emailedAt()).isNotNull();
        verify(contractEmailGateway).sendDocument(document, "recipient@example.com");
    }

    @Test
    void ownerCanLoadApprovalReview() {
        authenticateAs(RoleName.OWNER, "user-owner");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        contract.setStatus(ContractStatus.PENDING_APPROVAL.name());
        contract.setApprovalStatus(ContractApprovalStatus.PENDING.name());
        contract.setPendingAction(ContractPendingAction.SUBMIT.name());
        contract.setTotalAmount(new BigDecimal("600000000.00"));
        contract.setRequiresApproval(true);
        contract.setPriceChangePercent(new BigDecimal("12.00"));
        ContractApprovalEntity approval = new ContractApprovalEntity();
        approval.setId("approval-1");
        approval.setContract(contract);
        approval.setStatus(ContractApprovalStatus.PENDING.name());
        approval.setApprovalType("SUBMISSION");
        approval.setApprovalTier("OWNER");
        approval.setRequestedBy("user-accountant");
        contract.setApprovals(new ArrayList<>(List.of(approval)));

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(contractCreditGateway.getCreditSnapshot(customer)).thenReturn(
                new ContractCreditGateway.CreditSnapshot(new BigDecimal("700000000.00"), new BigDecimal("100000000.00"), new BigDecimal("600000000.00"), false)
        );

        var response = contractService.getApprovalReview("contract-1");

        assertThat(response.approvalRequest().approvalId()).isEqualTo("approval-1");
        assertThat(response.approvalRequest().pendingAction()).isEqualTo(ContractPendingAction.SUBMIT.name());
        assertThat(response.insights().approvalReasons()).anyMatch(reason -> reason.contains("500,000,000"));
        assertThat(response.insights().creditRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void getPendingApprovalsReturnsEnrichedFields() {
        authenticateAs(RoleName.OWNER, "user-owner");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        contract.setStatus(ContractStatus.PENDING_APPROVAL.name());
        contract.setApprovalStatus(ContractApprovalStatus.PENDING.name());
        contract.setPendingAction(ContractPendingAction.SUBMIT.name());
        ContractApprovalEntity approval = new ContractApprovalEntity();
        approval.setId("approval-1");
        approval.setContract(contract);
        approval.setStatus(ContractApprovalStatus.PENDING.name());
        approval.setApprovalType("SUBMISSION");
        approval.setRequestedBy("user-accountant");
        contract.setApprovals(new ArrayList<>(List.of(approval)));

        when(contractRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contract), PageRequest.of(0, 20), 1));

        PendingContractApprovalListQuery query = new PendingContractApprovalListQuery();
        query.setPendingAction(ContractPendingAction.SUBMIT.name());
        query.setPage(1);
        query.setPageSize(20);

        var response = contractService.getPendingApprovals(query);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).approvalStatus()).isEqualTo(ContractApprovalStatus.PENDING.name());
        assertThat(response.items().get(0).requestedBy()).isEqualTo("user-accountant");
        assertThat(response.filters().pendingAction()).isEqualTo(ContractPendingAction.SUBMIT.name());
    }

    @Test
    void cancelCompletedContractBlocked() {
        authenticateAs(RoleName.ACCOUNTANT, "user-accountant");
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ContractEntity contract = savedContract("contract-1", customer, product);
        contract.setStatus(ContractStatus.COMPLETED.name());

        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));

        var request = new com.g90.backend.modules.contract.dto.ContractCancelRequest();
        request.setCancellationReason(com.g90.backend.modules.contract.entity.ContractCancellationReason.CUSTOMER_REQUEST);

        assertThatThrownBy(() -> contractService.cancelContract("contract-1", request))
                .isInstanceOf(com.g90.backend.exception.ContractCancelNotAllowedException.class);
        verify(contractInventoryGateway, never()).releaseReservation(any(), anyString());
    }

    private void authenticateAs(RoleName roleName, String userId) {
        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(userId, userId + "@example.com", roleName.name(), "token"));
    }

    private ContractCreateRequest createRequest() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setCustomerId("customer-1");
        request.setPaymentTerms("70% on delivery, 30% within 30 days");
        request.setDeliveryAddress("Warehouse district 9");
        request.setDeliveryTerms("Standard delivery");
        request.setItems(List.of(itemRequest("product-1", "9.00", "1000.00")));
        return request;
    }

    private ContractItemRequest itemRequest(String productId, String quantity, String unitPrice) {
        ContractItemRequest request = new ContractItemRequest();
        request.setProductId(productId);
        request.setQuantity(new BigDecimal(quantity));
        request.setUnitPrice(new BigDecimal(unitPrice));
        return request;
    }

    private CustomerProfileEntity customer(String customerId, String companyName) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(customerId);
        customer.setCompanyName(companyName);
        customer.setStatus("ACTIVE");
        customer.setCreditLimit(new BigDecimal("1000000.00"));
        customer.setCreatedAt(LocalDateTime.now(APP_ZONE).minusMonths(7));
        return customer;
    }

    private ProductEntity product(String productId, String productCode) {
        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setProductCode(productCode);
        product.setProductName("Product " + productCode);
        product.setStatus(ProductStatus.ACTIVE.name());
        product.setUnit("PCS");
        return product;
    }

    private QuotationEntity quotation(String quotationId, CustomerProfileEntity customer, ProductEntity product) {
        QuotationEntity quotation = new QuotationEntity();
        quotation.setId(quotationId);
        quotation.setQuotationNumber("QT-20260315-0001");
        quotation.setCustomer(customer);
        quotation.setStatus(QuotationStatus.PENDING.name());
        quotation.setValidUntil(LocalDate.now(APP_ZONE).plusDays(10));
        quotation.setItems(new ArrayList<>(List.of(quotationItem(quotation, product))));
        return quotation;
    }

    private QuotationItemEntity quotationItem(QuotationEntity quotation, ProductEntity product) {
        QuotationItemEntity item = new QuotationItemEntity();
        item.setId("quotation-item-1");
        item.setQuotation(quotation);
        item.setProduct(product);
        item.setQuantity(new BigDecimal("9.00"));
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setTotalPrice(new BigDecimal("9000.00"));
        return item;
    }

    private ContractEntity savedContract(String contractId, CustomerProfileEntity customer, ProductEntity product) {
        ContractEntity contract = new ContractEntity();
        contract.setId(contractId);
        contract.setContractNumber("CT-20260315-0001");
        contract.setCustomer(customer);
        contract.setStatus(ContractStatus.DRAFT.name());
        contract.setApprovalStatus(ContractApprovalStatus.NOT_REQUIRED.name());
        contract.setPaymentTerms("70% on delivery, 30% within 30 days");
        contract.setDeliveryAddress("Warehouse district 9");
        contract.setTotalAmount(new BigDecimal("9000.00"));
        contract.setCreatedAt(LocalDateTime.now(APP_ZONE));
        contract.setUpdatedAt(LocalDateTime.now(APP_ZONE));

        ContractItemEntity item = new ContractItemEntity();
        item.setId("contract-item-1");
        item.setContract(contract);
        item.setProduct(product);
        item.setQuantity(new BigDecimal("9.00"));
        item.setBaseUnitPrice(new BigDecimal("1000.00"));
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        item.setTotalPrice(new BigDecimal("9000.00"));
        contract.setItems(new ArrayList<>(List.of(item)));
        contract.setVersions(new ArrayList<>());
        contract.setStatusHistory(new ArrayList<>());
        contract.setDocuments(new ArrayList<>());
        contract.setTrackingEvents(new ArrayList<>());
        contract.setApprovals(new ArrayList<>());
        return contract;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
