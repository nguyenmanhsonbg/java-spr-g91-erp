package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.entity.ContractApprovalType;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.email.service.NotificationEmailService;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailContractNotificationGateway implements ContractNotificationGateway {

    private final UserAccountRepository userAccountRepository;
    private final NotificationEmailService notificationEmailService;

    @Override
    public void notifyContractCreated(ContractEntity contract, String message) {
        NotificationEmailService.ContractCreatedEmailPayload payload =
                new NotificationEmailService.ContractCreatedEmailPayload(
                        contract.getContractNumber(),
                        contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                        contract.getTotalAmount(),
                        resolveUserDisplayName(contract.getCreatedBy()),
                        contract.getPaymentTerms(),
                        contract.getDeliveryAddress(),
                        StringUtils.hasText(contract.getNote()) ? contract.getNote().trim() : message
                );
        executeAfterCommit(() -> resolveStakeholders(contract).forEach(recipient ->
                notificationEmailService.sendContractCreatedEmail(recipient.email(), recipient.name(), payload)
        ));
    }

    @Override
    public void notifyContractApproved(ContractEntity contract, String message) {
        NotificationEmailService.ContractApprovedEmailPayload payload =
                new NotificationEmailService.ContractApprovedEmailPayload(
                        contract.getContractNumber(),
                        contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                        contract.getTotalAmount(),
                        resolveUserDisplayName(contract.getApprovedBy()),
                        contract.getApprovedAt(),
                        contract.getPaymentTerms(),
                        contract.getDeliveryAddress(),
                        StringUtils.hasText(contract.getNote()) ? contract.getNote().trim() : message
                );
        executeAfterCommit(() -> resolveStakeholders(contract).forEach(recipient ->
                notificationEmailService.sendContractApprovedEmail(recipient.email(), recipient.name(), payload)
        ));
    }

    @Override
    public void notifyWarehousePreparation(ContractEntity contract, String message) {
        log.debug("Warehouse preparation notification skipped for contract={} message={}", contract.getContractNumber(), message);
    }

    @Override
    public void notifyApprovalRequested(ContractEntity contract, ContractApprovalType approvalType, String message) {
        log.debug("Approval request notification skipped for contract={} approvalType={} message={}",
                contract.getContractNumber(), approvalType, message);
    }

    @Override
    public void notifyApprovalDecision(ContractEntity contract, String decision, String message) {
        log.debug("Approval decision notification skipped for contract={} decision={} message={}",
                contract.getContractNumber(), decision, message);
    }

    @Override
    public void notifyCancellation(ContractEntity contract, String message) {
        log.debug("Cancellation notification skipped for contract={} message={}", contract.getContractNumber(), message);
    }

    private List<Recipient> resolveStakeholders(ContractEntity contract) {
        Map<String, Recipient> recipientsByEmail = new LinkedHashMap<>();
        addUserRecipient(recipientsByEmail, contract.getCreatedBy());
        addCustomerRecipient(recipientsByEmail, contract.getCustomer());
        userAccountRepository.findByRole_NameIgnoreCaseAndStatusIgnoreCase(RoleName.OWNER.name(), AccountStatus.ACTIVE.name())
                .forEach(user -> addRecipient(recipientsByEmail, user.getEmail(), user.getFullName()));
        return recipientsByEmail.values().stream().toList();
    }

    private void addUserRecipient(Map<String, Recipient> recipientsByEmail, String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        userAccountRepository.findWithRoleById(userId)
                .ifPresent(user -> addRecipient(recipientsByEmail, user.getEmail(), user.getFullName()));
    }

    private void addCustomerRecipient(Map<String, Recipient> recipientsByEmail, CustomerProfileEntity customer) {
        if (customer == null) {
            return;
        }
        String email = StringUtils.hasText(customer.getEmail())
                ? customer.getEmail()
                : customer.getUser() == null ? null : customer.getUser().getEmail();
        String name = StringUtils.hasText(customer.getContactPerson())
                ? customer.getContactPerson()
                : customer.getCompanyName();
        addRecipient(recipientsByEmail, email, name);
    }

    private void addRecipient(Map<String, Recipient> recipientsByEmail, String email, String name) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        recipientsByEmail.putIfAbsent(normalizedEmail, new Recipient(normalizedEmail, normalizeName(name)));
    }

    private String resolveUserDisplayName(String userId) {
        if (!StringUtils.hasText(userId)) {
            return "G90 Steel";
        }
        Optional<UserAccountEntity> user = userAccountRepository.findWithRoleById(userId);
        return user.map(value -> normalizeName(value.getFullName())).orElse("G90 Steel");
    }

    private String normalizeName(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void executeAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private record Recipient(String email, String name) {
    }
}
