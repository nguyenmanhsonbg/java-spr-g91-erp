package com.g90.backend.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.debt.entity.DebtInvoiceEntity;
import com.g90.backend.modules.debt.entity.DebtReminderEntity;
import com.g90.backend.modules.debt.repository.DebtInvoiceRepository;
import com.g90.backend.modules.debt.repository.DebtReminderRepository;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.email.config.EmailProperties;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.payment.dto.InvoiceDueReminderRunResult;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceDueReminderServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private DebtReminderRepository debtReminderRepository;

    @Mock
    private DebtInvoiceRepository debtInvoiceRepository;

    @Mock
    private EmailService emailService;

    private InvoiceDueReminderServiceImpl service;

    @BeforeEach
    void setUp() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.setCompanyName("G90 Steel");
        emailProperties.setSupportEmail("support@g90steel.vn");
        service = new InvoiceDueReminderServiceImpl(
                invoiceRepository,
                paymentAllocationRepository,
                debtReminderRepository,
                debtInvoiceRepository,
                emailService,
                emailProperties,
                0,
                "Asia/Ho_Chi_Minh"
        );
    }

    @Test
    void sendDueInvoiceRemindersSendsOneEmailPerCustomerAndRecordsReminder() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        CustomerProfileEntity customer = customer("customer-1", "Alpha Steel", "billing@example.com");
        InvoiceEntity invoice = invoice("invoice-1", "INV-2026-0001", customer, today, "ISSUED", "1000.00", "100.00");

        when(invoiceRepository.findDueReminderCandidates(eq(today), eq(today), anyCollection())).thenReturn(List.of(invoice));
        when(debtReminderRepository.findInvoiceIdsByReminderType(anyCollection(), eq("DUE"))).thenReturn(List.of());
        when(paymentAllocationRepository.summarizeByInvoiceIds(anyCollection())).thenReturn(List.of(allocation("invoice-1", "200.00")));
        when(emailService.sendHtmlEmail(eq("billing@example.com"), eq("Invoice payment due today - Alpha Steel"), eq("email/invoice-due-reminder"), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(debtInvoiceRepository.getReferenceById("invoice-1")).thenReturn(debtInvoice("invoice-1"));
        when(debtReminderRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceDueReminderRunResult result = service.sendDueInvoiceReminders();

        assertThat(result.candidateInvoiceCount()).isEqualTo(1);
        assertThat(result.sentEmailCount()).isEqualTo(1);
        assertThat(result.sentReminderCount()).isEqualTo(1);
        assertThat(result.skippedInvoiceCount()).isZero();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendHtmlEmail(
                eq("billing@example.com"),
                eq("Invoice payment due today - Alpha Steel"),
                eq("email/invoice-due-reminder"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue().get("totalOutstanding")).isEqualTo(new BigDecimal("900.00"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DebtReminderEntity>> remindersCaptor = ArgumentCaptor.forClass(List.class);
        verify(debtReminderRepository).saveAll(remindersCaptor.capture());
        assertThat(remindersCaptor.getValue()).hasSize(1);
        assertThat(remindersCaptor.getValue().get(0).getReminderType()).isEqualTo("DUE");
        assertThat(remindersCaptor.getValue().get(0).getSentBy()).isEqualTo("SYSTEM");
    }

    @Test
    void sendDueInvoiceRemindersSkipsInvoiceThatAlreadyHasDueReminder() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        CustomerProfileEntity customer = customer("customer-1", "Alpha Steel", "billing@example.com");
        InvoiceEntity invoice = invoice("invoice-1", "INV-2026-0001", customer, today, "ISSUED", "1000.00", "100.00");

        when(invoiceRepository.findDueReminderCandidates(eq(today), eq(today), anyCollection())).thenReturn(List.of(invoice));
        when(debtReminderRepository.findInvoiceIdsByReminderType(anyCollection(), eq("DUE"))).thenReturn(List.of("invoice-1"));
        when(paymentAllocationRepository.summarizeByInvoiceIds(anyCollection())).thenReturn(List.of());

        InvoiceDueReminderRunResult result = service.sendDueInvoiceReminders();

        assertThat(result.candidateInvoiceCount()).isEqualTo(1);
        assertThat(result.sentEmailCount()).isZero();
        assertThat(result.sentReminderCount()).isZero();
        assertThat(result.skippedInvoiceCount()).isEqualTo(1);
        verify(emailService, never()).sendHtmlEmail(eq("billing@example.com"), anyString(), anyString(), anyMap());
        verify(debtReminderRepository, never()).saveAll(anyList());
    }

    private CustomerProfileEntity customer(String id, String companyName, String email) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(id);
        customer.setCompanyName(companyName);
        customer.setContactPerson("Nguyen Van A");
        customer.setEmail(email);
        return customer;
    }

    private InvoiceEntity invoice(
            String id,
            String invoiceNumber,
            CustomerProfileEntity customer,
            LocalDate dueDate,
            String status,
            String totalAmount,
            String vatAmount
    ) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(id);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomer(customer);
        invoice.setDueDate(dueDate);
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setVatAmount(new BigDecimal(vatAmount));
        return invoice;
    }

    private DebtInvoiceEntity debtInvoice(String id) {
        DebtInvoiceEntity invoice = new DebtInvoiceEntity();
        invoice.setId(id);
        return invoice;
    }

    private PaymentAllocationRepository.InvoiceAllocationTotalView allocation(String invoiceId, String amount) {
        return new PaymentAllocationRepository.InvoiceAllocationTotalView() {
            @Override
            public String getInvoiceId() {
                return invoiceId;
            }

            @Override
            public BigDecimal getAllocatedAmount() {
                return new BigDecimal(amount);
            }
        };
    }
}
