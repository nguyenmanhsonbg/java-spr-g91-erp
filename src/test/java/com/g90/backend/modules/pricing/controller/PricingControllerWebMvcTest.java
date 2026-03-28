package com.g90.backend.modules.pricing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.g90.backend.config.SecurityConfig;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.pricing.dto.PriceListListItemResponse;
import com.g90.backend.modules.pricing.dto.PriceListListResponseData;
import com.g90.backend.modules.pricing.service.PricingService;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.BearerTokenAuthenticationFilter;
import com.g90.backend.security.RestAccessDeniedHandler;
import com.g90.backend.security.RestAuthenticationEntryPoint;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PricingController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class PricingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PricingService pricingService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void accountantCanViewPriceListList() throws Exception {
        authenticateAs(RoleName.ACCOUNTANT);
        when(pricingService.getPriceLists(any())).thenReturn(listResponse());

        mockMvc.perform(get("/api/price-lists").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].name").value("April Pricing"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    @Test
    void customerCannotViewPriceListList() throws Exception {
        authenticateAs(RoleName.CUSTOMER);

        mockMvc.perform(get("/api/price-lists").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pricingService);
    }

    private void authenticateAs(RoleName roleName) {
        when(accessTokenService.extractBearerToken("Bearer token")).thenReturn("token");
        when(accessTokenService.resolveUserId("token")).thenReturn("user-1");
        when(userAccountRepository.findWithRoleById("user-1")).thenReturn(Optional.of(user("user-1", roleName)));
    }

    private PriceListListResponseData listResponse() {
        return new PriceListListResponseData(
                List.of(PriceListListItemResponse.builder()
                        .id("price-list-1")
                        .name("April Pricing")
                        .customerGroup("CONTRACTOR")
                        .validFrom(LocalDate.of(2026, 4, 1))
                        .validTo(LocalDate.of(2026, 4, 30))
                        .status("ACTIVE")
                        .itemCount(2)
                        .createdAt(LocalDateTime.of(2026, 3, 28, 9, 0))
                        .updatedAt(LocalDateTime.of(2026, 3, 28, 9, 30))
                        .build()),
                PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                new PriceListListResponseData.Filters(null, "ACTIVE", null, null, null)
        );
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
