package com.g90.backend.modules.promotion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.g90.backend.config.SecurityConfig;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.promotion.dto.PromotionDetailResponse;
import com.g90.backend.modules.promotion.dto.PromotionListItemResponse;
import com.g90.backend.modules.promotion.dto.PromotionListResponseData;
import com.g90.backend.modules.promotion.dto.PromotionScopeProductResponse;
import com.g90.backend.modules.promotion.service.PromotionService;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.BearerTokenAuthenticationFilter;
import com.g90.backend.security.RestAccessDeniedHandler;
import com.g90.backend.security.RestAuthenticationEntryPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(PromotionController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class PromotionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromotionService promotionService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void ownerCanViewPromotionList() throws Exception {
        authenticateAs(RoleName.OWNER);
        when(promotionService.getPromotions(any())).thenReturn(listResponse("ACTIVE"));

        mockMvc.perform(get("/api/promotions").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].name").value("April Promo"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    @Test
    void customerCanViewRelevantPromotions() throws Exception {
        authenticateAs(RoleName.CUSTOMER);
        when(promotionService.getPromotions(any())).thenReturn(listResponse("ACTIVE"));

        mockMvc.perform(get("/api/promotions").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].scopeSummary").value("1 product(s) | DEFAULT"));
    }

    @Test
    void ownerCanViewPromotionDetail() throws Exception {
        authenticateAs(RoleName.OWNER);
        when(promotionService.getPromotionById("promotion-1")).thenReturn(detailResponse());

        mockMvc.perform(get("/api/promotions/promotion-1").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value("promotion-1"))
                .andExpect(jsonPath("$.data.code").value("PROMO-APR-01"))
                .andExpect(jsonPath("$.data.products[0].productCode").value("SP001"))
                .andExpect(jsonPath("$.data.customerGroups[0]").value("DEFAULT"));
    }

    @Test
    void warehouseCannotViewPromotions() throws Exception {
        authenticateAs(RoleName.WAREHOUSE);

        mockMvc.perform(get("/api/promotions").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(promotionService);
    }

    @Test
    void unauthorizedUserCannotAccessOwnerOnlyApis() throws Exception {
        authenticateAs(RoleName.CUSTOMER);

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "April Promo",
                                  "promotionType": "PERCENT",
                                  "discountValue": 5.0,
                                  "validFrom": "2026-04-01",
                                  "validTo": "2026-04-30"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/promotions/promotion-1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "April Promo",
                                  "promotionType": "PERCENT",
                                  "discountValue": 5.0,
                                  "validFrom": "2026-04-01",
                                  "validTo": "2026-04-30"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/promotions/promotion-1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(promotionService);
    }

    private void authenticateAs(RoleName roleName) {
        when(accessTokenService.extractBearerToken("Bearer token")).thenReturn("token");
        when(accessTokenService.resolveUserId("token")).thenReturn("user-1");
        when(userAccountRepository.findWithRoleById("user-1")).thenReturn(Optional.of(user("user-1", roleName)));
    }

    private PromotionListResponseData listResponse(String status) {
        return new PromotionListResponseData(
                List.of(PromotionListItemResponse.builder()
                        .id("promotion-1")
                        .code("PROMO-APR-01")
                        .name("April Promo")
                        .promotionType("PERCENT")
                        .discountValue(new BigDecimal("5.00"))
                        .validFrom(LocalDate.of(2026, 4, 1))
                        .validTo(LocalDate.of(2026, 4, 30))
                        .status(status)
                        .priority(5)
                        .scopeSummary("1 product(s) | DEFAULT")
                        .productCount(1)
                        .customerGroups(List.of("DEFAULT"))
                        .createdAt(LocalDateTime.of(2026, 3, 28, 10, 0))
                        .updatedAt(LocalDateTime.of(2026, 3, 28, 10, 30))
                        .build()),
                PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                new PromotionListResponseData.Filters(null, status, null, null, null, null, null)
        );
    }

    private PromotionDetailResponse detailResponse() {
        return PromotionDetailResponse.builder()
                .id("promotion-1")
                .code("PROMO-APR-01")
                .name("April Promo")
                .promotionType("PERCENT")
                .discountValue(new BigDecimal("5.00"))
                .validFrom(LocalDate.of(2026, 4, 1))
                .validTo(LocalDate.of(2026, 4, 30))
                .status("ACTIVE")
                .priority(5)
                .description("Seasonal discount")
                .createdBy("owner-1")
                .updatedBy("owner-1")
                .createdAt(LocalDateTime.of(2026, 3, 28, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 28, 10, 30))
                .products(List.of(PromotionScopeProductResponse.builder()
                        .productId("product-1")
                        .productCode("SP001")
                        .productName("Steel Product SP001")
                        .build()))
                .customerGroups(List.of("DEFAULT"))
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
