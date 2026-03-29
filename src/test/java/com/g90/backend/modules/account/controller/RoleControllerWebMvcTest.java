package com.g90.backend.modules.account.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.g90.backend.config.SecurityConfig;
import com.g90.backend.modules.account.dto.RoleResponse;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.account.service.RoleService;
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

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class RoleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void ownerCanGetRoles() throws Exception {
        authenticateAs(RoleName.OWNER);
        when(roleService.getRoles()).thenReturn(List.of(
                new RoleResponse("22222222-2222-2222-2222-222222222222", "ACCOUNTANT", "Accountant role"),
                new RoleResponse("33333333-3333-3333-3333-333333333333", "WAREHOUSE", "Warehouse role")
        ));

        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].name").value("ACCOUNTANT"))
                .andExpect(jsonPath("$.data[1].name").value("WAREHOUSE"));
    }

    @Test
    void nonOwnerCannotGetRoles() throws Exception {
        authenticateAs(RoleName.ACCOUNTANT);

        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roleService);
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
