package com.g90.backend.modules.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InvoiceNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.debt.entity.PaymentEntity;
import com.g90.backend.modules.debt.service.PaymentExecutionService;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestConfirmRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestCreateRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestRejectRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestResponse;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.entity.PaymentConfirmationRequestEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.payment.repository.PaymentConfirmationRequestRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmationRequestServiceImplTest {

    @Mock
    private PaymentConfirmationRequestRepository paymentConfirmationRequestRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private PaymentExecutionService paymentExecutionService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EmailService emailService;

    private PaymentConfirmationRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentConfirmationRequestServiceImpl(
                paymentConfirmationRequestRepository,
                invoiceRepository,
                customerProfileRepository,
                invoiceService,
                paymentExecutionService,
                currentUserProvider,
                auditLogRepository,
                new ObjectMapper().findAndRegisterModules(),
                emailService
        );
    }

    @Test
    void createRequestSuccess() {
        CustomerProfileEntity customer = customer("customer-1", "user-1", "C001", "Alpha Steel", null);
        InvoiceEntity invoiceEntity = invoiceEntity("invoice-1", "INV-2026-0001", customer);
        InvoiceResponse invoice = invoiceResponse("invoice-1", "INV-2026-0001", "customer-1", "C001", "Alpha Steel", "ISSUED", "1000.00", "200.00", "800.00");
        PaymentConfirmationRequestCreateRequest request = createRequest("300.00");
        PaymentConfirmationRequestEntity savedEntity = pendingRequest("request-1", invoiceEntity, customer, "300.00");

        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser("user-1", "customer@example.com", "CUSTOMER", "token"));
        when(customerProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(customer));
        when(invoiceService.getInvoice("invoice-1")).thenReturn(invoice);
        when(paymentConfirmationRequestRepository.existsByInvoice_IdAndStatus("invoice-1", "PENDING_REVIEW")).thenReturn(false);
        when(invoiceRepository.findDetailedById("invoice-1")).thenReturn(Optional.of(invoiceEntity));
        when(paymentConfirmationRequestRepository.save(any(PaymentConfirmationRequestEntity.class))).thenReturn(savedEntity);

        PaymentConfirmationRequestResponse response = service.createRequest("invoice-1", request);

        assertEquals("request-1", response.id());
        assertEquals("PENDING_REVIEW", response.status());
        assertEquals(new BigDecimal("300.00"), response.requestedAmount());
        verify(auditLogRepository).save(any());
    }

    @Test
    void createRequestInvalidInvoice() {
        CustomerProfileEntity customer = customer("customer-1", "user-1", "C001", "Alpha Steel", null);
        PaymentConfirmationRequestCreateRequest request = createRequest("300.00");

        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser("user-1", "customer@example.com", "CUSTOMER", "token"));
        when(customerProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(customer));
        when(invoiceService.getInvoice("missing-invoice")).thenThrow(new InvoiceNotFoundException());

        assertThrows(InvoiceNotFoundException.class, () -> service.createRequest("missing-invoice", request));
    }

    @Test
    void confirmRequestSuccess() {
        CustomerProfileEntity customer = customer("customer-1", "user-1", "C001", "Alpha Steel", null);
        InvoiceEntity invoiceEntity = invoiceEntity("invoice-1", "INV-2026-0001", customer);
        PaymentConfirmationRequestEntity entity = pendingRequest("request-1", invoiceEntity, customer, "300.00");
        InvoiceResponse beforeInvoice = invoiceResponse("invoice-1", "INV-2026-0001", "customer-1", "C001", "Alpha Steel", "ISSUED", "1000.00", "200.00", "800.00");
        InvoiceResponse afterInvoice = invoiceResponse("invoice-1", "INV-2026-0001", "customer-1", "C001", "Alpha Steel", "PARTIALLY_PAID", "1000.00", "500.00", "500.00");
        PaymentConfirmationRequestConfirmRequest request = new PaymentConfirmationRequestConfirmRequest();
        request.setConfirmedAmount(new BigDecimal("300.00"));
        request.setReviewNote("Matched bank statement");
        PaymentEntity payment = payment("payment-1", "customer-1", "300.00");
        PaymentConfirmationRequestEntity confirmed = confirmedRequest(
                pendingRequest("request-1", invoiceEntity, customer, "300.00"),
                "payment-1",
                "300.00",
                "Matched bank statement"
        );

        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser("acc-1", "acc@example.com", "ACCOUNTANT", "token"));
        when(paymentConfirmationRequestRepository.findDetailedById("request-1")).thenReturn(Optional.of(entity));
        when(invoiceService.getInvoice("invoice-1")).thenReturn(beforeInvoice, afterInvoice);
        when(paymentExecutionService.recordPayment(any())).thenReturn(payment);
        when(paymentConfirmationRequestRepository.save(any(PaymentConfirmationRequestEntity.class))).thenReturn(confirmed);

        PaymentConfirmationRequestResponse response = service.confirmRequest("request-1", request);

        assertEquals("CONFIRMED", response.status());
        assertEquals("payment-1", response.paymentId());
        assertEquals(new BigDecimal("300.00"), response.confirmedAmount());
        verify(paymentExecutionService).recordPayment(any());
        verify(auditLogRepository, times(3)).save(any());
    }

    @Test
    void rejectRequestSuccess() {
        CustomerProfileEntity customer = customer("customer-1", "user-1", "C001", "Alpha Steel", null);
        InvoiceEntity invoiceEntity = invoiceEntity("invoice-1", "INV-2026-0001", customer);
        PaymentConfirmationRequestEntity entity = pendingRequest("request-1", invoiceEntity, customer, "300.00");
        PaymentConfirmationRequestEntity rejected = rejectedRequest(
                pendingRequest("request-1", invoiceEntity, customer, "300.00"),
                "Unable to match transfer"
        );
        InvoiceResponse invoice = invoiceResponse("invoice-1", "INV-2026-0001", "customer-1", "C001", "Alpha Steel", "ISSUED", "1000.00", "200.00", "800.00");
        PaymentConfirmationRequestRejectRequest request = new PaymentConfirmationRequestRejectRequest();
        request.setReason("Unable to match transfer");

        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser("owner-1", "owner@example.com", "OWNER", "token"));
        when(paymentConfirmationRequestRepository.findDetailedById("request-1")).thenReturn(Optional.of(entity));
        when(invoiceService.getInvoice("invoice-1")).thenReturn(invoice);
        when(paymentConfirmationRequestRepository.save(any(PaymentConfirmationRequestEntity.class))).thenReturn(rejected);

        PaymentConfirmationRequestResponse response = service.rejectRequest("request-1", request);

        assertEquals("REJECTED", response.status());
        assertEquals("Unable to match transfer", response.reviewNote());
        verify(paymentExecutionService, never()).recordPayment(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void duplicateConfirmBlocked() {
        CustomerProfileEntity customer = customer("customer-1", "user-1", "C001", "Alpha Steel", null);
        InvoiceEntity invoiceEntity = invoiceEntity("invoice-1", "INV-2026-0001", customer);
        PaymentConfirmationRequestEntity entity = confirmedRequest(
                pendingRequest("request-1", invoiceEntity, customer, "300.00"),
                "payment-1",
                "300.00",
                "Matched"
        );
        PaymentConfirmationRequestConfirmRequest request = new PaymentConfirmationRequestConfirmRequest();
        request.setConfirmedAmount(new BigDecimal("300.00"));

        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser("acc-1", "acc@example.com", "ACCOUNTANT", "token"));
        when(paymentConfirmationRequestRepository.findDetailedById("request-1")).thenReturn(Optional.of(entity));

        assertThrows(RequestValidationException.class, () -> service.confirmRequest("request-1", request));
        verify(paymentExecutionService, never()).recordPayment(any());
    }

    @Test
    void createRequestForNonCustomerIsForbidden() {
        PaymentConfirmationRequestCreateRequest request = createRequest("300.00");

        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser("acc-1", "acc@example.com", "ACCOUNTANT", "token"));

        assertThrows(ForbiddenOperationException.class, () -> service.createRequest("invoice-1", request));
        verify(paymentConfirmationRequestRepository, never()).save(any());
    }

    private PaymentConfirmationRequestCreateRequest createRequest(String amount) {
        PaymentConfirmationRequestCreateRequest request = new PaymentConfirmationRequestCreateRequest();
        request.setRequestedAmount(new BigDecimal(amount));
        request.setTransferTime(LocalDateTime.of(2026, 4, 15, 9, 30));
        request.setSenderBankName("ACB");
        request.setSenderAccountName("Nguyen Van A");
        request.setSenderAccountNo("0123456789");
        request.setReferenceCode("REF-001");
        request.setNote("Submitted by customer");
        return request;
    }

    private CustomerProfileEntity customer(String id, String userId, String code, String companyName, String email) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(id);
        customer.setCustomerCode(code);
        customer.setCompanyName(companyName);
        customer.setEmail(email);
        if (userId != null) {
            var user = new com.g90.backend.modules.account.entity.UserAccountEntity();
            user.setId(userId);
            customer.setUser(user);
        }
        return customer;
    }

    private InvoiceEntity invoiceEntity(String id, String invoiceNumber, CustomerProfileEntity customer) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(id);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomer(customer);
        return invoice;
    }

    private InvoiceResponse invoiceResponse(
            String invoiceId,
            String invoiceNumber,
            String customerId,
            String customerCode,
            String customerName,
            String status,
            String grandTotal,
            String paidAmount,
            String outstandingAmount
    ) {
        return new InvoiceResponse(
                invoiceId,
                invoiceNumber,
                "CONTRACT",
                "contract-1",
                "CT-001",
                customerId,
                customerCode,
                customerName,
                "0301234567",
                "Billing address",
                "Net 30",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("900.00"),
                new BigDecimal("0.00"),
                new BigDecimal("900.00"),
                new BigDecimal("11.11"),
                new BigDecimal("100.00"),
                new BigDecimal(grandTotal),
                new BigDecimal(paidAmount),
                new BigDecimal(outstandingAmount),
                status,
                null,
                null,
                null,
                "user-1",
                "user-1",
                "user-1",
                null,
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 1, 10, 0),
                null,
                null,
                List.of(),
                List.of()
        );
    }

    private PaymentConfirmationRequestEntity pendingRequest(String id, InvoiceEntity invoice, CustomerProfileEntity customer, String amount) {
        PaymentConfirmationRequestEntity entity = new PaymentConfirmationRequestEntity();
        entity.setId(id);
        entity.setInvoice(invoice);
        entity.setCustomer(customer);
        entity.setRequestedAmount(new BigDecimal(amount));
        entity.setTransferTime(LocalDateTime.of(2026, 4, 15, 9, 30));
        entity.setSenderBankName("ACB");
        entity.setSenderAccountName("Nguyen Van A");
        entity.setSenderAccountNo("0123456789");
        entity.setReferenceCode("REF-001");
        entity.setStatus("PENDING_REVIEW");
        entity.setCreatedBy("user-1");
        entity.setUpdatedBy("user-1");
        entity.setCreatedAt(LocalDateTime.of(2026, 4, 15, 9, 31));
        entity.setUpdatedAt(LocalDateTime.of(2026, 4, 15, 9, 31));
        return entity;
    }

    private PaymentConfirmationRequestEntity confirmedRequest(
            PaymentConfirmationRequestEntity entity,
            String paymentId,
            String confirmedAmount,
            String reviewNote
    ) {
        entity.setStatus("CONFIRMED");
        entity.setPaymentId(paymentId);
        entity.setConfirmedAmount(new BigDecimal(confirmedAmount));
        entity.setReviewNote(reviewNote);
        entity.setReviewedBy("acc-1");
        entity.setReviewedAt(LocalDateTime.of(2026, 4, 15, 10, 0));
        entity.setUpdatedBy("acc-1");
        entity.setUpdatedAt(LocalDateTime.of(2026, 4, 15, 10, 0));
        return entity;
    }

    private PaymentConfirmationRequestEntity rejectedRequest(PaymentConfirmationRequestEntity entity, String reason) {
        entity.setStatus("REJECTED");
        entity.setReviewNote(reason);
        entity.setReviewedBy("owner-1");
        entity.setReviewedAt(LocalDateTime.of(2026, 4, 15, 10, 0));
        entity.setUpdatedBy("owner-1");
        entity.setUpdatedAt(LocalDateTime.of(2026, 4, 15, 10, 0));
        return entity;
    }

    private PaymentEntity payment(String id, String customerId, String amount) {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(id);
        payment.setCustomerId(customerId);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(LocalDate.of(2026, 4, 15));
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setReferenceNo("REF-001");
        payment.setStatus("CONFIRMED");
        payment.setCreatedBy("acc-1");
        payment.setUpdatedBy("acc-1");
        payment.setCreatedAt(LocalDateTime.of(2026, 4, 15, 10, 0));
        payment.setUpdatedAt(LocalDateTime.of(2026, 4, 15, 10, 0));
        return payment;
    }
}
