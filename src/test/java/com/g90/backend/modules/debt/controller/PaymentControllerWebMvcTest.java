package com.g90.backend.modules.debt.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.debt.dto.PaymentCreateRequest;
import com.g90.backend.modules.debt.dto.PaymentResponse;
import com.g90.backend.modules.debt.service.PaymentService;
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

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canRecordPayment() throws Exception {
        when(paymentService.recordPayment(any())).thenReturn(paymentResponse());

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value("payment-1"))
                .andExpect(jsonPath("$.data.allocations[0].invoiceNumber").value("INV-001"));
    }

    @Test
    void canGetPaymentDetail() throws Exception {
        when(paymentService.getPayment(anyString())).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/payments/payment-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.receiptNumber").value("RCPT-ABC12345"));
    }

    private PaymentCreateRequest createRequest() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setCustomerId("customer-1");
        request.setPaymentDate(LocalDate.of(2026, 4, 3));
        request.setAmount(new BigDecimal("120.00"));
        request.setPaymentMethod("BANK_TRANSFER");
        request.setReferenceNo("TXN-001");
        return request;
    }

    private PaymentResponse paymentResponse() {
        return new PaymentResponse(
                "payment-1",
                "RCPT-ABC12345",
                "customer-1",
                "CUST-001",
                "ABC Steel",
                LocalDate.of(2026, 4, 3),
                new BigDecimal("120.00"),
                "BANK_TRANSFER",
                "TXN-001",
                "Paid by transfer",
                "accountant-1",
                LocalDateTime.of(2026, 4, 3, 10, 30),
                List.of(
                        new PaymentResponse.AllocationItem("invoice-1", "INV-001", new BigDecimal("100.00"), BigDecimal.ZERO, "PAID"),
                        new PaymentResponse.AllocationItem("invoice-2", "INV-002", new BigDecimal("20.00"), new BigDecimal("80.00"), "PARTIALLY_PAID")
                )
        );
    }
}
