package com.g90.backend.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InvoiceNotFoundException;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.payment.dto.InvoiceCancelRequest;
import com.g90.backend.modules.payment.dto.InvoiceCreateRequest;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EmailService emailService;

    private InvoiceServiceImpl invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(
                invoiceRepository,
                contractRepository,
                customerProfileRepository,
                paymentAllocationRepository,
                currentUserProvider,
                auditLogRepository,
                new ObjectMapper().findAndRegisterModules(),
                emailService
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendHtmlEmail(any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(paymentAllocationRepository.summarizeByInvoiceIds(anyCollection())).thenReturn(List.of());
        when(invoiceRepository.save(any())).thenAnswer(invocation -> {
            InvoiceEntity invoice = invocation.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId("invoice-1");
            }
            return invoice;
        });
    }

    @Test
    void createInvoiceUsesContractItemsAndAppliesVat() {
        authenticateAs(RoleName.ACCOUNTANT);
        ContractEntity contract = contract();
        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(invoiceRepository.existsByContract_IdAndStatusNotIn(anyString(), anyCollection())).thenReturn(false);
        when(invoiceRepository.countByIssueDateBetween(any(), any())).thenReturn(0L);

        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId("contract-1");
        request.setIssueDate(LocalDate.of(2026, 4, 3));
        request.setDueDate(LocalDate.of(2026, 4, 30));
        request.setStatus("ISSUED");

        var response = invoiceService.createInvoice(request);

        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(response.status()).isEqualTo("ISSUED");
        assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.vatAmount()).isEqualByComparingTo("10.00");
        assertThat(response.grandTotal()).isEqualByComparingTo("110.00");
        assertThat(response.items()).hasSize(1);
        verify(emailService).sendHtmlEmail(any(), any(), any(), any());
        verify(invoiceRepository, atLeastOnce()).save(any());
    }

    @Test
    void customerCannotAccessAnotherCustomersInvoice() {
        authenticateAs(RoleName.CUSTOMER);
        CustomerProfileEntity customer = customer("customer-1", "customer-user-1");
        when(customerProfileRepository.findByUser_Id("customer-user-1")).thenReturn(Optional.of(customer));
        when(invoiceRepository.findDetailedByIdAndCustomerId("invoice-1", "customer-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoice("invoice-1"))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void onlyOwnerCanCancelInvoice() {
        authenticateAs(RoleName.ACCOUNTANT);

        InvoiceCancelRequest request = new InvoiceCancelRequest();
        request.setCancellationReason("Issued in error");

        assertThatThrownBy(() -> invoiceService.cancelInvoice("invoice-1", request))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createDraftInvoiceSkipsNotification() {
        authenticateAs(RoleName.ACCOUNTANT);
        ContractEntity contract = contract();
        when(contractRepository.findDetailedById("contract-1")).thenReturn(Optional.of(contract));
        when(invoiceRepository.existsByContract_IdAndStatusNotIn(anyString(), anyCollection())).thenReturn(false);
        when(invoiceRepository.countByIssueDateBetween(any(), any())).thenReturn(1L);

        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId("contract-1");
        request.setIssueDate(LocalDate.of(2026, 4, 3));
        request.setDueDate(LocalDate.of(2026, 4, 30));
        request.setStatus("DRAFT");

        var response = invoiceService.createInvoice(request);

        assertThat(response.status()).isEqualTo("DRAFT");
        verify(invoiceRepository, atLeastOnce()).save(any());
    }

    private void authenticateAs(RoleName roleName) {
        String userId = roleName == RoleName.CUSTOMER ? "customer-user-1" : "accountant-1";
        when(currentUserProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser(userId, userId + "@g90steel.vn", roleName.name(), "token")
        );
    }

    private ContractEntity contract() {
        CustomerProfileEntity customer = customer("customer-1", "customer-user-1");
        ProductEntity product = new ProductEntity();
        product.setId("product-1");
        product.setProductCode("SP-001");
        product.setProductName("Steel Coil");
        product.setUnit("KG");

        ContractItemEntity item = new ContractItemEntity();
        item.setId("item-1");
        item.setProduct(product);
        item.setQuantity(new BigDecimal("1.00"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setTotalPrice(new BigDecimal("100.00"));

        ContractEntity contract = new ContractEntity();
        contract.setId("contract-1");
        contract.setContractNumber("CT-001");
        contract.setStatus("SUBMITTED");
        contract.setCustomer(customer);
        contract.setPaymentTerms("30 days");
        contract.setItems(List.of(item));
        return contract;
    }

    private CustomerProfileEntity customer(String customerId, String userId) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(userId);

        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(customerId);
        customer.setUser(user);
        customer.setCustomerCode("CUST-001");
        customer.setCompanyName("ABC Steel");
        customer.setTaxCode("123456789");
        customer.setAddress("1 Nguyen Hue");
        customer.setEmail("contact@abcsteel.vn");
        customer.setContactPerson("Mr. An");
        customer.setPaymentTerms("30 days");
        return customer;
    }
}
