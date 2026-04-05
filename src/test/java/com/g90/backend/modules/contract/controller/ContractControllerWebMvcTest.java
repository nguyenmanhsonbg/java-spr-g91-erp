package com.g90.backend.modules.contract.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.dto.ContractApprovalReviewResponseData;
import com.g90.backend.modules.contract.dto.ContractCreateRequest;
import com.g90.backend.modules.contract.dto.ContractDetailResponseData;
import com.g90.backend.modules.contract.dto.ContractFormInitResponseData;
import com.g90.backend.modules.contract.dto.ContractItemRequest;
import com.g90.backend.modules.contract.dto.ContractResponse;
import com.g90.backend.modules.contract.service.ContractService;
import com.g90.backend.security.AccessTokenService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContractController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContractControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContractService contractService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canCreateContract() throws Exception {
        when(contractService.createContract(any())).thenReturn(detailResponse());

        mockMvc.perform(post("/api/contracts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.contract.contractNumber").value("CT-20260315-0001"));
    }

    @Test
    void canViewContractDetail() throws Exception {
        when(contractService.getContractDetail(anyString())).thenReturn(detailResponse());

        mockMvc.perform(get("/api/contracts/contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.contract.id").value("contract-1"));
    }

    @Test
    void canLoadContractFormInit() throws Exception {
        when(contractService.getContractFormInit(any())).thenReturn(formInitResponse());

        mockMvc.perform(get("/api/contracts/form-init").param("customerId", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customer.id").value("customer-1"))
                .andExpect(jsonPath("$.data.defaults.suggestedPaymentTerms").value("70% on delivery, 30% within 30 days"));
    }

    @Test
    void canLoadApprovalReview() throws Exception {
        when(contractService.getApprovalReview(anyString())).thenReturn(approvalReviewResponse());

        mockMvc.perform(get("/api/contracts/contract-1/approval-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.approvalRequest.approvalId").value("approval-1"));
    }

    private ContractCreateRequest createRequest() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setCustomerId("customer-1");
        request.setPaymentTerms("70% on delivery, 30% within 30 days");
        request.setDeliveryAddress("Warehouse district 9");
        request.setItems(List.of(itemRequest()));
        return request;
    }

    private ContractItemRequest itemRequest() {
        ContractItemRequest item = new ContractItemRequest();
        item.setProductId("product-1");
        item.setQuantity(new BigDecimal("10.00"));
        item.setUnitPrice(new BigDecimal("1000.00"));
        return item;
    }

    private ContractDetailResponseData detailResponse() {
        return new ContractDetailResponseData(
                new ContractResponse(
                        "contract-1",
                        "CT-20260315-0001",
                        null,
                        "customer-1",
                        "Customer A",
                        null,
                        "DRAFT",
                        "NOT_REQUIRED",
                        null,
                        false,
                        false,
                        "70% on delivery, 30% within 30 days",
                        "Warehouse district 9",
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("10000.00"),
                        new BigDecimal("1000000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("20.00"),
                        new BigDecimal("2000.00"),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null,
                        null,
                        null,
                        LocalDateTime.now().plusDays(7)
                ),
                new ContractDetailResponseData.CustomerData("customer-1", "Customer A", "CONTRACTOR", new BigDecimal("1000000.00"), "ACTIVE"),
                null,
                List.of(),
                new ContractDetailResponseData.ApprovalData(false, "NOT_REQUIRED", null, null, null, null, null, null),
                new ContractDetailResponseData.CreditData(new BigDecimal("1000000.00"), BigDecimal.ZERO, new BigDecimal("10000.00"), new BigDecimal("990000.00")),
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    private ContractFormInitResponseData formInitResponse() {
        return new ContractFormInitResponseData(
                new ContractFormInitResponseData.CustomerData(
                        "customer-1",
                        "Customer A",
                        "CONTRACTOR",
                        "Ms A",
                        "0909000000",
                        "customer@example.com",
                        "District 9",
                        new BigDecimal("1000000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("990000.00"),
                        new BigDecimal("20.00")
                ),
                null,
                new ContractFormInitResponseData.DefaultsData(
                        "70% on delivery, 30% within 30 days",
                        "District 9",
                        null
                ),
                List.of(),
                List.of()
        );
    }

    private ContractApprovalReviewResponseData approvalReviewResponse() {
        return new ContractApprovalReviewResponseData(
                detailResponse(),
                new ContractApprovalReviewResponseData.ApprovalRequestData(
                        "approval-1",
                        "SUBMISSION",
                        "OWNER",
                        "PENDING",
                        "SUBMIT",
                        "user-accountant",
                        LocalDateTime.now(),
                        LocalDateTime.now().plusHours(24),
                        "Need owner review"
                ),
                new ContractApprovalReviewResponseData.ReviewInsights(
                        List.of("Contract total exceeds 500,000,000 VND approval threshold"),
                        8,
                        "MEDIUM",
                        new BigDecimal("12.00"),
                        new BigDecimal("700000000.00"),
                        new BigDecimal("100000000.00"),
                        new BigDecimal("600000000.00"),
                        new BigDecimal("100000000.00"),
                        "TODO: integrate cost and margin aggregator for full profitability review.",
                        "Review credit exposure, negotiated pricing, and delivery/payment terms before deciding."
                )
        );
    }
}
