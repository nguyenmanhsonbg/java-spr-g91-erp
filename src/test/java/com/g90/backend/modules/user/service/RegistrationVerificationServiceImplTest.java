package com.g90.backend.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.exception.RegistrationVerificationCodeExpiredException;
import com.g90.backend.exception.RegistrationVerificationCodeInvalidException;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.entity.CustomerStatus;
import com.g90.backend.modules.email.config.EmailProperties;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.entity.EmailVerificationTokenEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.modules.user.repository.EmailVerificationTokenRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
class RegistrationVerificationServiceImplTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationVerificationServiceImpl registrationVerificationService;

    @BeforeEach
    void setUp() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.getVerification().setExpireMinutes(10);
        emailProperties.getVerification().setResendCooldownSeconds(60);
        registrationVerificationService = new RegistrationVerificationServiceImpl(
                userAccountRepository,
                emailVerificationTokenRepository,
                customerProfileRepository,
                passwordEncoder,
                emailProperties
        );
    }

    @Test
    void verifyWithCorrectCodeSucceeds() {
        UserAccountEntity user = pendingUser();
        CustomerProfileEntity customer = pendingCustomer(user);
        EmailVerificationTokenEntity token = activeToken(user);
        token.setCodeHash("encoded-code");
        token.setExpiredAt(LocalDateTime.now(APP_ZONE).plusMinutes(5));

        when(userAccountRepository.findWithRoleByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findFirstByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc("user-1"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("A2B3C", "encoded-code")).thenReturn(true);
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(customer));
        when(customerProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = registrationVerificationService.verify("customer@example.com", "a2b3c");

        assertThat(response.verified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(token.getConsumedAt()).isNotNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE.name());
    }

    @Test
    void verifyWithWrongCodeFails() {
        UserAccountEntity user = pendingUser();
        EmailVerificationTokenEntity token = activeToken(user);
        token.setCodeHash("encoded-code");
        token.setExpiredAt(LocalDateTime.now(APP_ZONE).plusMinutes(5));

        when(userAccountRepository.findWithRoleByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findFirstByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc("user-1"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("A2B3C", "encoded-code")).thenReturn(false);

        assertThatThrownBy(() -> registrationVerificationService.verify("customer@example.com", "A2B3C"))
                .isInstanceOf(RegistrationVerificationCodeInvalidException.class);
    }

    @Test
    void verifyWithExpiredCodeFails() {
        UserAccountEntity user = pendingUser();
        EmailVerificationTokenEntity token = activeToken(user);
        token.setExpiredAt(LocalDateTime.now(APP_ZONE).minusMinutes(1));

        when(userAccountRepository.findWithRoleByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findFirstByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc("user-1"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> registrationVerificationService.verify("customer@example.com", "A2B3C"))
                .isInstanceOf(RegistrationVerificationCodeExpiredException.class);
        assertThat(token.getInvalidatedAt()).isNotNull();
    }

    @Test
    void resendInvalidatesPreviousCode() {
        UserAccountEntity user = pendingUser();
        EmailVerificationTokenEntity existingToken = activeToken(user);
        existingToken.setCreatedAt(LocalDateTime.now(APP_ZONE).minusMinutes(2));

        when(userAccountRepository.findWithRoleByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findFirstByUser_IdOrderByCreatedAtDesc("user-1")).thenReturn(Optional.of(existingToken));
        when(emailVerificationTokenRepository.findByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNull("user-1"))
                .thenReturn(List.of(existingToken));
        when(passwordEncoder.encode(any())).thenReturn("encoded-new-code");
        when(emailVerificationTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = registrationVerificationService.resend("customer@example.com");

        assertThat(existingToken.getInvalidatedAt()).isNotNull();
        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.verificationCode()).hasSize(5);

        ArgumentCaptor<EmailVerificationTokenEntity> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
        verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUser().getId()).isEqualTo("user-1");
        assertThat(tokenCaptor.getValue().getExpiredAt()).isNotNull();
    }

    private UserAccountEntity pendingUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-1");
        user.setEmail("customer@example.com");
        user.setRole(role(RoleName.CUSTOMER));
        user.setStatus(AccountStatus.PENDING_VERIFICATION.name());
        user.setEmailVerified(Boolean.FALSE);
        return user;
    }

    private CustomerProfileEntity pendingCustomer(UserAccountEntity user) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId("customer-1");
        customer.setUser(user);
        customer.setStatus(CustomerStatus.PENDING_VERIFICATION.name());
        return customer;
    }

    private EmailVerificationTokenEntity activeToken(UserAccountEntity user) {
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setId("token-1");
        token.setUser(user);
        token.setCreatedAt(LocalDateTime.now(APP_ZONE).minusMinutes(1));
        token.setUpdatedAt(LocalDateTime.now(APP_ZONE).minusMinutes(1));
        return token;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
