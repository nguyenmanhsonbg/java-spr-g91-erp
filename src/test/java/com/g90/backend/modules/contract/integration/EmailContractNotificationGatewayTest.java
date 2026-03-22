package com.g90.backend.modules.contract.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.email.service.NotificationEmailService;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailContractNotificationGatewayTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private NotificationEmailService notificationEmailService;

    @Test
    void notifyContractCreatedSendsToCreatorCustomerAndOwner() {
        EmailContractNotificationGateway gateway = new EmailContractNotificationGateway(userAccountRepository, notificationEmailService);
        ContractEntity contract = contract();
        contract.setCreatedBy("user-accountant");

        when(userAccountRepository.findWithRoleById("user-accountant")).thenReturn(Optional.of(user("user-accountant", RoleName.ACCOUNTANT, "accountant@example.com", "Accountant A")));
        when(userAccountRepository.findByRole_NameIgnoreCaseAndStatusIgnoreCase(RoleName.OWNER.name(), AccountStatus.ACTIVE.name()))
                .thenReturn(List.of(user("user-owner", RoleName.OWNER, "owner@example.com", "Owner A")));

        gateway.notifyContractCreated(contract, "created");

        verify(notificationEmailService, times(3)).sendContractCreatedEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(NotificationEmailService.ContractCreatedEmailPayload.class)
        );
    }

    @Test
    void notifyContractApprovedUsesApproverNameInPayload() {
        EmailContractNotificationGateway gateway = new EmailContractNotificationGateway(userAccountRepository, notificationEmailService);
        ContractEntity contract = contract();
        contract.setCreatedBy("user-accountant");
        contract.setApprovedBy("user-owner");
        contract.setApprovedAt(LocalDateTime.of(2026, 3, 22, 14, 0));

        when(userAccountRepository.findWithRoleById("user-accountant")).thenReturn(Optional.of(user("user-accountant", RoleName.ACCOUNTANT, "accountant@example.com", "Accountant A")));
        when(userAccountRepository.findWithRoleById("user-owner")).thenReturn(Optional.of(user("user-owner", RoleName.OWNER, "owner@example.com", "Owner A")));
        when(userAccountRepository.findByRole_NameIgnoreCaseAndStatusIgnoreCase(RoleName.OWNER.name(), AccountStatus.ACTIVE.name()))
                .thenReturn(List.of(user("user-owner", RoleName.OWNER, "owner@example.com", "Owner A")));

        gateway.notifyContractApproved(contract, "approved");

        ArgumentCaptor<NotificationEmailService.ContractApprovedEmailPayload> payloadCaptor =
                ArgumentCaptor.forClass(NotificationEmailService.ContractApprovedEmailPayload.class);
        verify(notificationEmailService, times(3)).sendContractApprovedEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue().approvedByName()).isEqualTo("Owner A");
    }

    private ContractEntity contract() {
        ContractEntity contract = new ContractEntity();
        contract.setId("contract-1");
        contract.setContractNumber("CT-20260322-0001");
        contract.setTotalAmount(new BigDecimal("125000000.00"));
        contract.setPaymentTerms("70% on delivery, 30% within 30 days");
        contract.setDeliveryAddress("District 9");
        contract.setNote("Priority customer");

        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId("customer-1");
        customer.setCompanyName("Customer A");
        customer.setContactPerson("Nguyen Van A");
        customer.setEmail("customer@example.com");
        contract.setCustomer(customer);
        return contract;
    }

    private UserAccountEntity user(String id, RoleName roleName, String email, String fullName) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(id);
        user.setRole(role(roleName));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setEmailVerified(Boolean.TRUE);
        return user;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
