package com.g90.backend.modules.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.dto.CustomerCreateRequest;
import com.g90.backend.modules.customer.dto.CustomerCreateResponse;
import com.g90.backend.modules.customer.dto.CustomerDetailResponseData;
import com.g90.backend.modules.customer.dto.CustomerDisableRequest;
import com.g90.backend.modules.customer.dto.CustomerListResponseData;
import com.g90.backend.modules.customer.dto.CustomerResponse;
import com.g90.backend.modules.customer.dto.CustomerStatusResponse;
import com.g90.backend.modules.customer.dto.CustomerSummaryResponseData;
import com.g90.backend.modules.customer.service.CustomerService;
import com.g90.backend.modules.product.dto.PaginationResponse;
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

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canCreateCustomer() throws Exception {
        when(customerService.createCustomer(any())).thenReturn(
                new CustomerCreateResponse("customer-1", "CUST-001", "ABC Steel", "ACTIVE", null)
        );

        mockMvc.perform(post("/api/customers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerCode").value("CUST-001"));
    }

    @Test
    void canGetCustomerList() throws Exception {
        when(customerService.getCustomers(any())).thenReturn(
                new CustomerListResponseData(
                        List.of(new CustomerListResponseData.Item(
                                "customer-1",
                                "CUST-001",
                                "ABC Steel",
                                "0101234567",
                                "Nguyen Van A",
                                "0901234567",
                                "contact@abcsteel.vn",
                                "CONTRACTOR",
                                "CONTRACTOR",
                                new BigDecimal("500000000.00"),
                                "ACTIVE",
                                false,
                                LocalDateTime.now()
                        )),
                        PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                        new CustomerListResponseData.Filters(null, null, null, null, null, null, null, null)
                )
        );

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].customerCode").value("CUST-001"));
    }

    @Test
    void canGetCustomerDetail() throws Exception {
        when(customerService.getCustomerDetail(anyString())).thenReturn(detailResponse());

        mockMvc.perform(get("/api/customers/customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customer.id").value("customer-1"));
    }

    @Test
    void canDisableCustomer() throws Exception {
        when(customerService.disableCustomer(anyString(), any())).thenReturn(
                new CustomerStatusResponse("customer-1", "CUST-001", "INACTIVE", "Customer inactive", LocalDateTime.now())
        );

        CustomerDisableRequest request = new CustomerDisableRequest();
        request.setReason("Customer inactive");

        mockMvc.perform(patch("/api/customers/customer-1/disable")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void canGetCustomerSummary() throws Exception {
        when(customerService.getCustomerSummary(anyString())).thenReturn(
                new CustomerSummaryResponseData(
                        "customer-1",
                        "CUST-001",
                        "ABC Steel",
                        "ACTIVE",
                        new BigDecimal("500000000.00"),
                        "70% on delivery, 30% within 30 days",
                        new BigDecimal("100000000.00"),
                        new BigDecimal("20000000.00"),
                        new BigDecimal("20000000.00"),
                        new BigDecimal("80000000.00"),
                        2,
                        1,
                        1,
                        1,
                        0,
                        0,
                        true,
                        List.of(),
                        LocalDateTime.now()
                )
        );

        mockMvc.perform(get("/api/customers/customer-1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerCode").value("CUST-001"));
    }

    private CustomerCreateRequest createRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCompanyName("ABC Steel");
        request.setTaxCode("0101234567");
        request.setCustomerType("CONTRACTOR");
        request.setPriceGroup("CONTRACTOR");
        request.setCreditLimit(new BigDecimal("500000000.00"));
        return request;
    }

    private CustomerDetailResponseData detailResponse() {
        return new CustomerDetailResponseData(
                new CustomerResponse(
                        "customer-1",
                        "CUST-001",
                        "ABC Steel",
                        "0101234567",
                        "Ha Noi",
                        "Nguyen Van A",
                        "0901234567",
                        "contact@abcsteel.vn",
                        "CONTRACTOR",
                        "CONTRACTOR",
                        new BigDecimal("500000000.00"),
                        "70% on delivery, 30% within 30 days",
                        "ACTIVE",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ),
                null,
                new CustomerDetailResponseData.FinancialData(
                        new BigDecimal("500000000.00"),
                        "70% on delivery, 30% within 30 days",
                        new BigDecimal("100000000.00"),
                        new BigDecimal("20000000.00"),
                        new BigDecimal("20000000.00"),
                        new BigDecimal("80000000.00")
                ),
                new CustomerDetailResponseData.ActivityData(2, 1, 1, 1, 0, 0, LocalDateTime.now()),
                List.of(new CustomerDetailResponseData.ContactPersonData("Nguyen Van A", "0901234567", "contact@abcsteel.vn", true)),
                List.of(),
                List.of()
        );
    }
}
