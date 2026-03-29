package com.g90.backend.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.RoleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleRepository);
    }

    @Test
    void getRolesReturnsMappedResponses() {
        when(roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))).thenReturn(List.of(
                role("22222222-2222-2222-2222-222222222222", RoleName.ACCOUNTANT, "Accountant role"),
                role("33333333-3333-3333-3333-333333333333", RoleName.WAREHOUSE, "Warehouse role")
        ));

        var response = roleService.getRoles();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).name()).isEqualTo("ACCOUNTANT");
        assertThat(response.get(1).description()).isEqualTo("Warehouse role");
    }

    private RoleEntity role(String id, RoleName roleName, String description) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(roleName.name());
        role.setDescription(description);
        return role;
    }
}
