package com.g90.backend.modules.product.controller;

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
import com.g90.backend.modules.product.dto.ProductFiltersResponse;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.service.ProductService;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.BearerTokenAuthenticationFilter;
import com.g90.backend.security.RestAccessDeniedHandler;
import com.g90.backend.security.RestAuthenticationEntryPoint;
import java.math.BigDecimal;
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

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class ProductControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void guestCanViewProductList() throws Exception {
        when(productService.getProducts(any())).thenReturn(listResponse());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Steel Coil Prime"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    @Test
    void guestCanViewProductDetails() throws Exception {
        when(productService.getProductById("product-1")).thenReturn(productResponse());

        mockMvc.perform(get("/api/products/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("SP001"))
                .andExpect(jsonPath("$.data.productName").value("Steel Coil Prime"));
    }

    @Test
    void productListRejectsPageBelowOne() throws Exception {
        mockMvc.perform(get("/api/products?page=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(productService);
    }

    @Test
    void warehouseCanCreateProduct() throws Exception {
        authenticateAs(RoleName.WAREHOUSE);
        when(productService.createProduct(any())).thenReturn(productResponse());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "SP001",
                                  "productName": "Steel Coil Prime",
                                  "type": "COIL",
                                  "size": "1200 x 2400",
                                  "thickness": "0.45",
                                  "unit": "KG",
                                  "weightConversion": 1.25,
                                  "referenceWeight": 1.26,
                                  "description": "Primary warehouse coil"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.productCode").value("SP001"));
    }

    @Test
    void createProductMissingRequiredFieldsRejected() throws Exception {
        authenticateAs(RoleName.WAREHOUSE);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "",
                                  "productName": "",
                                  "type": "",
                                  "size": "",
                                  "thickness": "",
                                  "unit": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(productService);
    }

    @Test
    void guestCannotAccessCreateUpdateDeleteApis() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "SP001",
                                  "productName": "Steel Coil Prime",
                                  "type": "COIL",
                                  "size": "1200 x 2400",
                                  "thickness": "0.45",
                                  "unit": "KG"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/products/product-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "SP001",
                                  "productName": "Steel Coil Prime",
                                  "type": "COIL",
                                  "size": "1200 x 2400",
                                  "thickness": "0.45",
                                  "unit": "KG",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/products/product-1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    private void authenticateAs(RoleName roleName) {
        when(accessTokenService.extractBearerToken("Bearer token")).thenReturn("token");
        when(accessTokenService.resolveUserId("token")).thenReturn("user-1");
        when(userAccountRepository.findWithRoleById("user-1")).thenReturn(Optional.of(user("user-1", roleName)));
    }

    private ProductListResponseData listResponse() {
        return new ProductListResponseData(
                List.of(productResponse()),
                PaginationResponse.builder().page(1).pageSize(20).totalItems(1).totalPages(1).build(),
                ProductFiltersResponse.builder().keyword(null).search(null).type(null).size(null).thickness(null).unit(null).status("ACTIVE").build()
        );
    }

    private ProductResponse productResponse() {
        return ProductResponse.builder()
                .id("product-1")
                .productCode("SP001")
                .productName("Steel Coil Prime")
                .type("COIL")
                .size("1200 x 2400")
                .thickness("0.45")
                .unit("KG")
                .weightConversion(new BigDecimal("1.2500"))
                .referenceWeight(new BigDecimal("1.2600"))
                .description("Primary warehouse coil")
                .imageUrls(List.of("https://cdn.example.com/coil-1.jpg"))
                .status("ACTIVE")
                .createdAt(OffsetDateTime.parse("2026-03-28T09:00:00+07:00"))
                .updatedAt(OffsetDateTime.parse("2026-03-28T09:15:00+07:00"))
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
