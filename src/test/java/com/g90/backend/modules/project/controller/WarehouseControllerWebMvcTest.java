package com.g90.backend.modules.project.controller;

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
import com.g90.backend.modules.project.dto.WarehouseResponse;
import com.g90.backend.modules.project.service.WarehouseService;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.BearerTokenAuthenticationFilter;
import com.g90.backend.security.RestAccessDeniedHandler;
import com.g90.backend.security.RestAuthenticationEntryPoint;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WarehouseController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class WarehouseControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void warehouseCanGetWarehouses() throws Exception {
        authenticateAs(RoleName.WAREHOUSE);
        when(warehouseService.getWarehouses()).thenReturn(List.of(
                new WarehouseResponse("warehouse-1", "Main Warehouse", "HCMC"),
                new WarehouseResponse("warehouse-2", "Backup Warehouse", "Ha Noi")
        ));

        mockMvc.perform(get("/api/warehouses").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value("warehouse-1"))
                .andExpect(jsonPath("$.data[1].name").value("Backup Warehouse"));
    }

    @Test
    void customerCannotGetWarehouses() throws Exception {
        authenticateAs(RoleName.CUSTOMER);

        mockMvc.perform(get("/api/warehouses").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(warehouseService);
    }

    private void authenticateAs(RoleName roleName) {
        when(accessTokenService.extractBearerToken("Bearer token")).thenReturn("token");
        when(accessTokenService.resolveUserId("token")).thenReturn("user-1");
        when(userAccountRepository.findWithRoleById("user-1")).thenReturn(Optional.of(user("user-1", roleName)));
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
