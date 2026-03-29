package com.g90.backend.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.modules.account.dto.AccountDeactivateRequest;
import com.g90.backend.modules.account.dto.AccountListQuery;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.mapper.AccountMapper;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.RoleRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private final AccountMapper accountMapper = new AccountMapper();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(
                userAccountRepository,
                roleRepository,
                auditLogRepository,
                accountMapper,
                passwordEncoder,
                objectMapper
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getAccountsAllowsNullFiltersInAuditPayload() {
        AccountListQuery query = new AccountListQuery();
        when(userAccountRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(account("user-1", RoleName.ACCOUNTANT, AccountStatus.ACTIVE)),
                        PageRequest.of(0, 10),
                        1
                ));

        var response = accountService.getAccounts(query);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.content().get(0).role()).isEqualTo(RoleName.ACCOUNTANT.name());
        verify(auditLogRepository).save(any());
    }

    @Test
    void deactivateAccountAllowsNullReasonInAuditPayload() {
        UserAccountEntity account = account("user-2", RoleName.WAREHOUSE, AccountStatus.ACTIVE);
        when(userAccountRepository.findById("user-2")).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deactivateAccount("user-2", new AccountDeactivateRequest());

        assertThat(account.getStatus()).isEqualTo(AccountStatus.INACTIVE.name());
        verify(auditLogRepository).save(any());
    }

    private UserAccountEntity account(String id, RoleName roleName, AccountStatus status) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(id);
        user.setFullName("User " + id);
        user.setEmail(id + "@g90steel.vn");
        user.setStatus(status.name());
        user.setEmailVerified(Boolean.TRUE);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 29, 9, 0));
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
