package com.g90.backend.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.EmailVerificationRequiredException;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.RoleRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.entity.CustomerStatus;
import com.g90.backend.modules.email.service.NotificationEmailService;
import com.g90.backend.modules.user.dto.LoginRequest;
import com.g90.backend.modules.user.dto.RegisterRequest;
import com.g90.backend.modules.user.dto.ForgotPasswordRequest;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.entity.PasswordResetTokenEntity;
import com.g90.backend.modules.user.mapper.UserManagementMapper;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.modules.user.repository.PasswordResetTokenRepository;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.CurrentUserProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private RegistrationVerificationService registrationVerificationService;
    @Mock
    private NotificationEmailService notificationEmailService;

    private UserManagementServiceImpl userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementServiceImpl(
                userAccountRepository,
                roleRepository,
                auditLogRepository,
                customerProfileRepository,
                passwordResetTokenRepository,
                new UserManagementMapper(),
                passwordEncoder,
                new ObjectMapper().findAndRegisterModules(),
                accessTokenService,
                currentUserProvider,
                registrationVerificationService,
                notificationEmailService
        );
    }

    @Test
    void registerCreatesPendingAccountAndTriggersVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("Customer@Example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");

        RoleEntity customerRole = role(RoleName.CUSTOMER);
        when(roleRepository.findByNameIgnoreCase(RoleName.CUSTOMER.name())).thenReturn(Optional.of(customerRole));
        when(userAccountRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any())).thenAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(customerProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationVerificationService.issueForNewRegistration(any())).thenReturn(
                new RegistrationVerificationService.VerificationDispatch(
                        "user-1",
                        "customer@example.com",
                        "Nguyen Van A",
                        "A2B3C",
                        10
                )
        );

        var response = userManagementService.register(request);

        ArgumentCaptor<UserAccountEntity> userCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION.name());
        assertThat(userCaptor.getValue().getEmailVerified()).isFalse();
        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.expireMinutes()).isEqualTo(10);
        assertThat(response.email()).isEqualTo("customer@example.com");

        ArgumentCaptor<CustomerProfileEntity> customerCaptor = ArgumentCaptor.forClass(CustomerProfileEntity.class);
        verify(customerProfileRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION.name());

        verify(notificationEmailService).sendRegistrationVerificationEmail("customer@example.com", "Nguyen Van A", "A2B3C", 10);
    }

    @Test
    void loginIsBlockedForUnverifiedAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@example.com");
        request.setPassword("secret123");

        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-1");
        user.setEmail("customer@example.com");
        user.setRole(role(RoleName.CUSTOMER));
        user.setPasswordHash("encoded-password");
        user.setStatus(AccountStatus.PENDING_VERIFICATION.name());
        user.setEmailVerified(Boolean.FALSE);

        when(userAccountRepository.findWithRoleByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.login(request))
                .isInstanceOf(EmailVerificationRequiredException.class);
    }

    @Test
    void forgotPasswordCreatesTokenAndSendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("customer@example.com");

        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-1");
        user.setEmail("customer@example.com");
        user.setFullName("Nguyen Van A");
        user.setRole(role(RoleName.CUSTOMER));
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setEmailVerified(Boolean.TRUE);

        when(userAccountRepository.findWithRoleByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser_IdAndUsedFalse("user-1")).thenReturn(List.of());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        userManagementService.forgotPassword(request);

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isNotBlank();
        assertThat(tokenCaptor.getValue().getExpiredAt()).isNotNull();

        verify(notificationEmailService).sendPasswordResetEmail(
                org.mockito.ArgumentMatchers.eq("customer@example.com"),
                org.mockito.ArgumentMatchers.eq("Nguyen Van A"),
                any(NotificationEmailService.PasswordResetEmailPayload.class)
        );
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
