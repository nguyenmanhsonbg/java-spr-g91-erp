package com.g90.backend.modules.saleorder.controller;

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
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.saleorder.dto.SaleOrderActionResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderDetailResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderListResponseData;
import com.g90.backend.modules.saleorder.dto.SaleOrderTimelineResponseData;
import com.g90.backend.modules.saleorder.service.SaleOrderService;
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

@WebMvcTest(SaleOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class SaleOrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaleOrderService saleOrderService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canGetSaleOrderList() throws Exception {
        when(saleOrderService.getSaleOrders(any())).thenReturn(listResponse());

        mockMvc.perform(get("/api/sale-orders").param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].saleOrderNumber").value("SO-20260405-0001"));
    }

    @Test
    void canGetSaleOrderDetail() throws Exception {
        when(saleOrderService.getSaleOrder(anyString())).thenReturn(detailResponse());

        mockMvc.perform(get("/api/sale-orders/contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.header.saleOrderNumber").value("SO-20260405-0001"))
                .andExpect(jsonPath("$.data.inventoryIssues[0].transactionCode").value("INV-IS-202604051030-ABCD"));
    }

    @Test
    void canUpdateSaleOrderStatus() throws Exception {
        when(saleOrderService.updateStatus(anyString(), any())).thenReturn(actionResponse());

        mockMvc.perform(patch("/api/sale-orders/contract-1/status")
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PROCESSING",
                                  "note": "Warehouse started handling"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.currentStatus").value("PROCESSING"));
    }

    @Test
    void canCreateInvoiceFromSaleOrder() throws Exception {
        when(saleOrderService.createInvoice(anyString(), any())).thenReturn(invoiceResponse());

        mockMvc.perform(post("/api/sale-orders/contract-1/invoices")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                            put("dueDate", "2026-04-30");
                            put("status", "ISSUED");
                        }})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-2026-0001"));
    }

    private SaleOrderListResponseData listResponse() {
        return new SaleOrderListResponseData(
                List.of(new SaleOrderListResponseData.Item(
                        "contract-1",
                        "SO-20260405-0001",
                        "CT-20260405-0001",
                        "customer-1",
                        "ABC Steel",
                        "project-1",
                        "PRJ-001",
                        "Warehouse Upgrade",
                        LocalDate.of(2026, 4, 5),
                        LocalDate.of(2026, 4, 10),
                        null,
                        "SUBMITTED",
                        new BigDecimal("1000.00")
                )),
                PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                new SaleOrderListResponseData.Filters(null, null, null, null, null, "SUBMITTED", null, null, null, null)
        );
    }

    private SaleOrderDetailResponseData detailResponse() {
        return new SaleOrderDetailResponseData(
                new SaleOrderDetailResponseData.HeaderData(
                        "contract-1",
                        "SO-20260405-0001",
                        "contract-1",
                        "CT-20260405-0001",
                        "PROCESSING",
                        LocalDate.of(2026, 4, 5),
                        LocalDateTime.of(2026, 4, 5, 9, 0),
                        null,
                        LocalDate.of(2026, 4, 10),
                        null,
                        new BigDecimal("1000.00"),
                        "Order note"
                ),
                new SaleOrderDetailResponseData.CustomerData("customer-1", "CUST-001", "ABC Steel", "Mr An", "0909", "a@b.com", "HCM"),
                new SaleOrderDetailResponseData.ProjectData("project-1", "PRJ-001", "Warehouse Upgrade", "ACTIVE"),
                List.of(new SaleOrderDetailResponseData.ItemData(
                        "item-1",
                        "product-1",
                        "SP001",
                        "Steel Coil",
                        "COIL",
                        "1200",
                        "1.2",
                        "KG",
                        new BigDecimal("10.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("3.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("100.00"),
                        new BigDecimal("1000.00"),
                        null
                )),
                new SaleOrderDetailResponseData.FulfillmentSummaryData(
                        new BigDecimal("10.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("3.00"),
                        BigDecimal.ZERO,
                        1,
                        1,
                        1
                ),
                List.of(new SaleOrderDetailResponseData.TimelineEventData(
                        "PROCESSING_STARTED",
                        "PROCESSING",
                        "Order processing started",
                        "Warehouse started handling",
                        null,
                        LocalDateTime.of(2026, 4, 5, 10, 30),
                        null
                )),
                List.of(new SaleOrderDetailResponseData.InventoryIssueData(
                        "tx-1",
                        "INV-IS-202604051030-ABCD",
                        "product-1",
                        "SP001",
                        "Steel Coil",
                        new BigDecimal("3.00"),
                        LocalDateTime.of(2026, 4, 5, 10, 30),
                        "Delivery batch",
                        null
                )),
                List.of(new SaleOrderDetailResponseData.InvoiceData(
                        "invoice-1",
                        "INV-2026-0001",
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 4, 30),
                        new BigDecimal("1100.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("1100.00"),
                        "ISSUED",
                        null
                ))
        );
    }

    private SaleOrderActionResponseData actionResponse() {
        return new SaleOrderActionResponseData(
                "contract-1",
                "SO-20260405-0001",
                "CT-20260405-0001",
                "SUBMITTED",
                "PROCESSING",
                "NOT_REQUIRED",
                "PROCESSING",
                "warehouse-1",
                LocalDateTime.of(2026, 4, 5, 10, 30),
                "Warehouse started handling",
                null
        );
    }

    private InvoiceResponse invoiceResponse() {
        return new InvoiceResponse(
                "invoice-1",
                "INV-2026-0001",
                "CONTRACT",
                "contract-1",
                "CT-20260405-0001",
                "customer-1",
                "CUST-001",
                "ABC Steel",
                "123456789",
                "HCM",
                "30 days",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"),
                BigDecimal.ZERO,
                new BigDecimal("1100.00"),
                "ISSUED",
                null,
                null,
                null,
                "accountant-1",
                "accountant-1",
                "accountant-1",
                null,
                LocalDateTime.of(2026, 4, 10, 9, 0),
                LocalDateTime.of(2026, 4, 10, 9, 0),
                LocalDateTime.of(2026, 4, 10, 9, 0),
                null,
                null,
                List.of(),
                List.of()
        );
    }
}
