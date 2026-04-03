package com.g90.backend.modules.debt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.customer.repository.CustomerManagementRepository;
import com.g90.backend.modules.debt.dto.PaymentCreateRequest;
import com.g90.backend.modules.debt.entity.DebtInvoiceEntity;
import com.g90.backend.modules.debt.entity.PaymentEntity;
import com.g90.backend.modules.debt.repository.DebtInvoiceRepository;
import com.g90.backend.modules.debt.repository.DebtReminderRepository;
import com.g90.backend.modules.debt.repository.DebtSettlementRepository;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.debt.repository.PaymentRepository;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DebtServiceImplTest {

    @Mock
    private CustomerManagementRepository customerManagementRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private DebtInvoiceRepository debtInvoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;
    @Mock
    private DebtReminderRepository debtReminderRepository;
    @Mock
    private DebtSettlementRepository debtSettlementRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EmailService emailService;

    private DebtServiceImpl debtService;

    @BeforeEach
    void setUp() {
        debtService = new DebtServiceImpl(
                customerManagementRepository,
                customerProfileRepository,
                debtInvoiceRepository,
                paymentRepository,
                paymentAllocationRepository,
                debtReminderRepository,
                debtSettlementRepository,
                currentUserProvider,
                auditLogRepository,
                new ObjectMapper().findAndRegisterModules(),
                emailService
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendHtmlEmail(any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void recordPaymentAllocatesFifoAcrossOpenInvoices() {
        authenticateAs(RoleName.ACCOUNTANT);
        CustomerProfileEntity customer = customer();
        DebtInvoiceEntity firstInvoice = invoice("invoice-1", "INV-001", new BigDecimal("100.00"), LocalDate.of(2026, 3, 20));
        DebtInvoiceEntity secondInvoice = invoice("invoice-2", "INV-002", new BigDecimal("100.00"), LocalDate.of(2026, 3, 25));

        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer));
        when(debtInvoiceRepository.findByCustomerIdOrderByDueDateAscCreatedAtAsc("customer-1")).thenReturn(List.of(firstInvoice, secondInvoice));
        when(paymentAllocationRepository.summarizeByInvoiceIds(any())).thenReturn(
                List.of(),
                List.of(allocationView("invoice-1", new BigDecimal("100.00")), allocationView("invoice-2", new BigDecimal("20.00"))),
                List.of(allocationView("invoice-1", new BigDecimal("100.00")), allocationView("invoice-2", new BigDecimal("20.00")))
        );
        when(paymentRepository.existsDuplicate(any(), any(), any(), any(), any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            PaymentEntity payment = invocation.getArgument(0);
            payment.setId("payment-1");
            payment.setCreatedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
            return payment;
        });

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setCustomerId("customer-1");
        request.setPaymentDate(LocalDate.of(2026, 4, 3));
        request.setAmount(new BigDecimal("120.00"));
        request.setPaymentMethod("BANK_TRANSFER");
        request.setReferenceNo("TXN-001");

        var response = debtService.recordPayment(request);

        assertThat(response.id()).isEqualTo("payment-1");
        assertThat(response.allocations()).hasSize(2);
        assertThat(response.allocations().get(0).allocatedAmount()).isEqualByComparingTo("100.00");
        assertThat(response.allocations().get(1).allocatedAmount()).isEqualByComparingTo("20.00");
        assertThat(firstInvoice.getStatus()).isEqualTo("PAID");
        assertThat(secondInvoice.getStatus()).isEqualTo("PARTIALLY_PAID");
        verify(paymentRepository).save(any());
        verify(debtInvoiceRepository).saveAll(any());
    }

    @Test
    void customerCannotAccessAnotherCustomersDebt() {
        authenticateAs(RoleName.CUSTOMER);
        when(customerProfileRepository.findByUser_Id("customer-user-1")).thenReturn(Optional.of(customer()));

        assertThatThrownBy(() -> debtService.getDebtStatus("customer-2"))
                .isInstanceOf(CustomerProfileNotFoundException.class);
    }

    @Test
    void draftInvoicesAreExcludedFromOpenInvoiceList() {
        authenticateAs(RoleName.ACCOUNTANT);
        DebtInvoiceEntity draftInvoice = invoice("invoice-1", "INV-001", new BigDecimal("100.00"), LocalDate.of(2026, 4, 10));
        draftInvoice.setStatus("DRAFT");
        DebtInvoiceEntity issuedInvoice = invoice("invoice-2", "INV-002", new BigDecimal("200.00"), LocalDate.of(2026, 4, 15));
        issuedInvoice.setStatus("ISSUED");

        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer()));
        when(debtInvoiceRepository.findByCustomerIdOrderByDueDateAscCreatedAtAsc("customer-1")).thenReturn(List.of(draftInvoice, issuedInvoice));
        when(paymentAllocationRepository.summarizeByInvoiceIds(any())).thenReturn(List.of());

        var openInvoices = debtService.getOpenInvoices("customer-1");

        assertThat(openInvoices).hasSize(1);
        assertThat(openInvoices.get(0).invoiceNumber()).isEqualTo("INV-002");
    }

    private void authenticateAs(RoleName roleName) {
        String userId = roleName == RoleName.CUSTOMER ? "customer-user-1" : "accountant-1";
        when(currentUserProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser(userId, userId + "@g90steel.vn", roleName.name(), "token")
        );
    }

    private CustomerProfileEntity customer() {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId("customer-1");
        customer.setCustomerCode("CUST-001");
        customer.setCompanyName("ABC Steel");
        customer.setEmail("contact@abcsteel.vn");
        customer.setCreditLimit(new BigDecimal("500000000.00"));
        customer.setPaymentTerms("70% on delivery, 30% within 30 days");
        return customer;
    }

    private DebtInvoiceEntity invoice(String id, String invoiceNumber, BigDecimal totalAmount, LocalDate dueDate) {
        DebtInvoiceEntity invoice = new DebtInvoiceEntity();
        invoice.setId(id);
        invoice.setCustomerId("customer-1");
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTotalAmount(totalAmount);
        invoice.setVatAmount(BigDecimal.ZERO);
        invoice.setStatus("OPEN");
        invoice.setDueDate(dueDate);
        invoice.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        return invoice;
    }

    private PaymentAllocationRepository.InvoiceAllocationTotalView allocationView(String invoiceId, BigDecimal amount) {
        return new PaymentAllocationRepository.InvoiceAllocationTotalView() {
            @Override
            public String getInvoiceId() {
                return invoiceId;
            }

            @Override
            public BigDecimal getAllocatedAmount() {
                return amount;
            }
        };
    }
}
