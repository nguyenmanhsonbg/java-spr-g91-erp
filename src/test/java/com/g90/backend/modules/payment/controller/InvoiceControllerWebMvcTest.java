package com.g90.backend.modules.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.payment.dto.InvoiceCancelRequest;
import com.g90.backend.modules.payment.dto.InvoiceCreateRequest;
import com.g90.backend.modules.payment.dto.InvoiceListResponseData;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.InvoiceUpdateRequest;
import com.g90.backend.modules.payment.service.InvoiceService;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.security.AccessTokenService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canCreateInvoice() throws Exception {
        when(invoiceService.createInvoice(any())).thenReturn(invoiceResponse());

        mockMvc.perform(post("/api/invoices")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-2026-0001"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"));
    }

    @Test
    void canGetInvoiceList() throws Exception {
        when(invoiceService.getInvoices(any())).thenReturn(listResponse());

        mockMvc.perform(get("/api/invoices").param("status", "ISSUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].invoiceNumber").value("INV-2026-0001"));
    }

    @Test
    void canGetInvoiceDetail() throws Exception {
        when(invoiceService.getInvoice(anyString())).thenReturn(invoiceResponse());

        mockMvc.perform(get("/api/invoices/invoice-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerCode").value("CUST-001"))
                .andExpect(jsonPath("$.data.paymentHistory[0].receiptNumber").value("RCPT-ABC12345"));
    }

    @Test
    void canUpdateInvoice() throws Exception {
        when(invoiceService.updateInvoice(anyString(), any())).thenReturn(invoiceResponse());

        mockMvc.perform(put("/api/invoices/invoice-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.dueDate").value("2026-04-30"));
    }

    @Test
    void canCancelInvoice() throws Exception {
        when(invoiceService.cancelInvoice(anyString(), any())).thenReturn(cancelledInvoiceResponse());

        mockMvc.perform(post("/api/invoices/invoice-1/cancel")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cancelRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private InvoiceCreateRequest createRequest() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId("contract-1");
        request.setIssueDate(LocalDate.of(2026, 4, 3));
        request.setDueDate(LocalDate.of(2026, 4, 30));
        request.setStatus("ISSUED");
        return request;
    }

    private InvoiceUpdateRequest updateRequest() {
        InvoiceUpdateRequest request = new InvoiceUpdateRequest();
        request.setDueDate(LocalDate.of(2026, 4, 30));
        request.setNote("Updated note");
        return request;
    }

    private InvoiceCancelRequest cancelRequest() {
        InvoiceCancelRequest request = new InvoiceCancelRequest();
        request.setCancellationReason("Issued in error");
        return request;
    }

    private InvoiceListResponseData listResponse() {
        return new InvoiceListResponseData(
                List.of(new InvoiceListResponseData.Item(
                        "invoice-1",
                        "INV-2026-0001",
                        "CONTRACT",
                        "contract-1",
                        "CT-001",
                        "customer-1",
                        "CUST-001",
                        "ABC Steel",
                        LocalDate.of(2026, 4, 3),
                        LocalDate.of(2026, 4, 30),
                        new BigDecimal("110.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("110.00"),
                        "ISSUED",
                        null
                )),
                PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                new InvoiceListResponseData.Filters(null, null, null, null, null, "ISSUED")
        );
    }

    private InvoiceResponse invoiceResponse() {
        return new InvoiceResponse(
                "invoice-1",
                "INV-2026-0001",
                "CONTRACT",
                "contract-1",
                "CT-001",
                "customer-1",
                "CUST-001",
                "ABC Steel",
                "123456789",
                "1 Nguyen Hue",
                "30 days",
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                new BigDecimal("110.00"),
                BigDecimal.ZERO,
                new BigDecimal("110.00"),
                "ISSUED",
                "Invoice note",
                null,
                null,
                "accountant-1",
                "accountant-1",
                "accountant-1",
                null,
                LocalDateTime.of(2026, 4, 3, 10, 0),
                LocalDateTime.of(2026, 4, 3, 10, 0),
                LocalDateTime.of(2026, 4, 3, 10, 0),
                null,
                LocalDateTime.of(2026, 4, 3, 10, 1),
                List.of(new InvoiceResponse.Item(
                        "item-1",
                        "product-1",
                        "SP-001",
                        "Steel Coil",
                        "Steel Coil",
                        "KG",
                        new BigDecimal("1.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00")
                )),
                List.of(new InvoiceResponse.PaymentHistoryItem(
                        "payment-1",
                        "RCPT-ABC12345",
                        LocalDate.of(2026, 4, 10),
                        new BigDecimal("20.00"),
                        new BigDecimal("20.00"),
                        "BANK_TRANSFER",
                        "TXN-001",
                        "Partial payment",
                        "accountant-1",
                        LocalDateTime.of(2026, 4, 10, 11, 0)
                ))
        );
    }

    private InvoiceResponse cancelledInvoiceResponse() {
        return new InvoiceResponse(
                "invoice-1",
                "INV-2026-0001",
                "CONTRACT",
                "contract-1",
                "CT-001",
                "customer-1",
                "CUST-001",
                "ABC Steel",
                "123456789",
                "1 Nguyen Hue",
                "30 days",
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                new BigDecimal("110.00"),
                BigDecimal.ZERO,
                new BigDecimal("110.00"),
                "CANCELLED",
                "Invoice note",
                null,
                "Issued in error",
                "accountant-1",
                "owner-1",
                "accountant-1",
                "owner-1",
                LocalDateTime.of(2026, 4, 3, 10, 0),
                LocalDateTime.of(2026, 4, 3, 10, 5),
                LocalDateTime.of(2026, 4, 3, 10, 0),
                LocalDateTime.of(2026, 4, 3, 10, 5),
                LocalDateTime.of(2026, 4, 3, 10, 6),
                List.of(),
                List.of()
        );
    }
}
