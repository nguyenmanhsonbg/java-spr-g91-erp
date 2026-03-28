package com.g90.backend.modules.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.g90.backend.config.SecurityConfig;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.inventory.dto.InventoryHistoryItemResponse;
import com.g90.backend.modules.inventory.dto.InventoryHistoryResponseData;
import com.g90.backend.modules.inventory.dto.InventoryMutationResponse;
import com.g90.backend.modules.inventory.dto.InventoryPaginationResponse;
import com.g90.backend.modules.inventory.dto.InventoryStatusItemResponse;
import com.g90.backend.modules.inventory.dto.InventoryStatusResponseData;
import com.g90.backend.modules.inventory.service.InventoryService;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.BearerTokenAuthenticationFilter;
import com.g90.backend.security.RestAccessDeniedHandler;
import com.g90.backend.security.RestAuthenticationEntryPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class InventoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void warehouseCanViewInventoryStatusSuccess() throws Exception {
        authenticateAs(RoleName.WAREHOUSE);
        when(inventoryService.getInventoryStatus(any())).thenReturn(statusResponse());

        mockMvc.perform(get("/api/inventory/status").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].productCode").value("SP001"))
                .andExpect(jsonPath("$.data.items[0].currentQuantity").value(120));
    }

    @Test
    void warehouseCanViewInventoryHistoryWithFilters() throws Exception {
        authenticateAs(RoleName.WAREHOUSE);
        when(inventoryService.getInventoryHistory(any())).thenReturn(historyResponse());

        mockMvc.perform(get("/api/inventory/history")
                        .header("Authorization", "Bearer token")
                        .queryParam("transactionType", "ISSUE")
                        .queryParam("fromDate", "2026-03-01")
                        .queryParam("toDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("ISSUE"))
                .andExpect(jsonPath("$.data.filters.transactionType").value("ISSUE"));
    }

    @Test
    void unauthorizedUserCannotAccessInventoryApis() throws Exception {
        authenticateAs(RoleName.CUSTOMER);

        mockMvc.perform(get("/api/inventory/status").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/inventory/receipts")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "product-1",
                                  "quantity": 100,
                                  "receiptDate": "2026-03-28T10:00:00",
                                  "reason": "Supplier delivery"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(inventoryService);
    }

    private void authenticateAs(RoleName roleName) {
        when(accessTokenService.extractBearerToken("Bearer token")).thenReturn("token");
        when(accessTokenService.resolveUserId("token")).thenReturn("user-1");
        when(userAccountRepository.findWithRoleById("user-1")).thenReturn(Optional.of(user("user-1", roleName)));
    }

    private InventoryStatusResponseData statusResponse() {
        return InventoryStatusResponseData.builder()
                .items(List.of(InventoryStatusItemResponse.builder()
                        .productId("product-1")
                        .productCode("SP001")
                        .productName("Steel Coil Prime")
                        .type("COIL")
                        .unit("KG")
                        .currentQuantity(new BigDecimal("120.00"))
                        .updatedAt(OffsetDateTime.parse("2026-03-28T14:30:00+07:00"))
                        .build()))
                .pagination(InventoryPaginationResponse.builder().page(1).size(20).totalItems(1).totalPages(1).build())
                .filters(InventoryStatusResponseData.Filters.builder().search(null).productId(null).build())
                .build();
    }

    @SuppressWarnings("unused")
    private InventoryMutationResponse mutationResponse() {
        return InventoryMutationResponse.builder()
                .transactionId("tx-1")
                .transactionCode("INV-RC-202603281430-ABCD")
                .transactionType("RECEIPT")
                .productId("product-1")
                .productCode("SP001")
                .productName("Steel Coil Prime")
                .quantity(new BigDecimal("100.00"))
                .quantityBefore(new BigDecimal("20.00"))
                .quantityAfter(new BigDecimal("120.00"))
                .transactionDate(OffsetDateTime.parse("2026-03-28T14:30:00+07:00"))
                .operatorId("user-1")
                .operatorEmail("warehouse@g90steel.vn")
                .reason("Supplier delivery")
                .build();
    }

    private InventoryHistoryResponseData historyResponse() {
        return InventoryHistoryResponseData.builder()
                .items(List.of(InventoryHistoryItemResponse.builder()
                        .transactionId("tx-2")
                        .transactionCode("INV-IS-202603281500-ABCD")
                        .transactionType("ISSUE")
                        .productId("product-1")
                        .productCode("SP001")
                        .productName("Steel Coil Prime")
                        .quantity(new BigDecimal("50.00"))
                        .quantityBefore(new BigDecimal("120.00"))
                        .quantityAfter(new BigDecimal("70.00"))
                        .transactionDate(OffsetDateTime.parse("2026-03-28T15:00:00+07:00"))
                        .operatorId("user-1")
                        .operatorEmail("warehouse@g90steel.vn")
                        .reason("Project delivery")
                        .note("Shipment batch A")
                        .build()))
                .pagination(InventoryPaginationResponse.builder().page(1).size(20).totalItems(1).totalPages(1).build())
                .filters(InventoryHistoryResponseData.Filters.builder()
                        .productId(null)
                        .transactionType("ISSUE")
                        .fromDate(LocalDate.of(2026, 3, 1))
                        .toDate(LocalDate.of(2026, 3, 31))
                        .build())
                .build();
    }

    private UserAccountEntity user(String userId, RoleName roleName) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(userId);
        user.setEmail(userId + "@g90steel.vn");
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setEmailVerified(Boolean.TRUE);
        user.setRole(role(roleName));
        return user;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
