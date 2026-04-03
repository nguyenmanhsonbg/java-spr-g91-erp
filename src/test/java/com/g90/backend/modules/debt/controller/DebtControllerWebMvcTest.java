package com.g90.backend.modules.debt.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.debt.dto.DebtAgingResponseData;
import com.g90.backend.modules.debt.dto.DebtListResponseData;
import com.g90.backend.modules.debt.dto.DebtSettlementListResponseData;
import com.g90.backend.modules.debt.dto.DebtStatusResponseData;
import com.g90.backend.modules.debt.dto.OpenInvoiceResponse;
import com.g90.backend.modules.debt.dto.PaymentResponse;
import com.g90.backend.modules.debt.dto.ReminderSendResponseData;
import com.g90.backend.modules.debt.dto.SettlementConfirmRequest;
import com.g90.backend.modules.debt.dto.SettlementResponse;
import com.g90.backend.modules.debt.service.DebtService;
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

@WebMvcTest(DebtController.class)
@AutoConfigureMockMvc(addFilters = false)
class DebtControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DebtService debtService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canGetDebtList() throws Exception {
        when(debtService.getDebts(any())).thenReturn(
                new DebtListResponseData(
                        List.of(new DebtListResponseData.Item(
                                "customer-1",
                                "CUST-001",
                                "ABC Steel",
                                new BigDecimal("500000000.00"),
                                new BigDecimal("120000000.00"),
                                new BigDecimal("30000000.00"),
                                new BigDecimal("90000000.00"),
                                new BigDecimal("20000000.00"),
                                new BigDecimal("10000000.00"),
                                BigDecimal.ZERO,
                                LocalDate.of(2026, 4, 3),
                                "OVERDUE"
                        )),
                        PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                        new DebtListResponseData.Filters(null, null, null, null, null, null)
                )
        );

        mockMvc.perform(get("/api/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].customerCode").value("CUST-001"));
    }

    @Test
    void canGetDebtStatus() throws Exception {
        when(debtService.getDebtStatus(anyString())).thenReturn(debtStatusResponse());

        mockMvc.perform(get("/api/debts/customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.summary.customerCode").value("CUST-001"))
                .andExpect(jsonPath("$.data.openInvoices[0].invoiceNumber").value("INV-001"));
    }

    @Test
    void canConfirmSettlement() throws Exception {
        when(debtService.confirmSettlement(anyString(), any())).thenReturn(settlementResponse());

        SettlementConfirmRequest request = new SettlementConfirmRequest();
        request.setNote("All invoices paid");

        mockMvc.perform(post("/api/debts/customer-1/settlement-confirmation")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("SETTLED"));
    }

    @Test
    void canExportDebts() throws Exception {
        when(debtService.exportDebts(any())).thenReturn("Customer Code\nCUST-001\n".getBytes());

        mockMvc.perform(get("/api/debts/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("debt-report.csv")));
    }

    private DebtStatusResponseData debtStatusResponse() {
        return new DebtStatusResponseData(
                new DebtStatusResponseData.Summary(
                        "customer-1",
                        "CUST-001",
                        "ABC Steel",
                        new BigDecimal("500000000.00"),
                        "70% on delivery, 30% within 30 days",
                        new BigDecimal("200000000.00"),
                        new BigDecimal("80000000.00"),
                        new BigDecimal("80000000.00"),
                        new BigDecimal("120000000.00"),
                        new BigDecimal("30000000.00"),
                        LocalDate.of(2026, 4, 3),
                        "OVERDUE"
                ),
                new DebtAgingResponseData(
                        new BigDecimal("120000000.00"),
                        new BigDecimal("30000000.00"),
                        new BigDecimal("90000000.00"),
                        new BigDecimal("20000000.00"),
                        new BigDecimal("10000000.00"),
                        BigDecimal.ZERO
                ),
                List.of(new OpenInvoiceResponse(
                        "invoice-1",
                        "INV-001",
                        LocalDateTime.of(2026, 3, 1, 10, 0),
                        LocalDate.of(2026, 3, 20),
                        new BigDecimal("100000000.00"),
                        new BigDecimal("50000000.00"),
                        new BigDecimal("50000000.00"),
                        "PARTIALLY_PAID",
                        14
                )),
                List.of(new PaymentResponse(
                        "payment-1",
                        "RCPT-ABC12345",
                        "customer-1",
                        "CUST-001",
                        "ABC Steel",
                        LocalDate.of(2026, 4, 3),
                        new BigDecimal("80000000.00"),
                        "BANK_TRANSFER",
                        "TXN-001",
                        null,
                        "accountant-1",
                        LocalDateTime.of(2026, 4, 3, 9, 0),
                        List.of()
                )),
                List.of(),
                List.of(settlementResponse())
        );
    }

    private SettlementResponse settlementResponse() {
        return new SettlementResponse(
                "settlement-1",
                "customer-1",
                "CUST-001",
                "ABC Steel",
                LocalDate.of(2026, 4, 3),
                "accountant-1",
                "All invoices paid",
                null,
                "SETTLED",
                LocalDateTime.of(2026, 4, 3, 11, 0)
        );
    }
}
