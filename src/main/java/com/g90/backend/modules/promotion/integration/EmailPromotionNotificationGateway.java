package com.g90.backend.modules.promotion.integration;

import com.g90.backend.modules.email.service.NotificationEmailService;
import com.g90.backend.modules.promotion.entity.PromotionCustomerGroupEntity;
import com.g90.backend.modules.promotion.entity.PromotionEntity;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class EmailPromotionNotificationGateway implements PromotionNotificationGateway {

    private final CustomerProfileRepository customerProfileRepository;
    private final NotificationEmailService notificationEmailService;

    @Override
    public void notifyPromotionCreated(PromotionEntity promotion) {
        NotificationEmailService.PromotionCreatedEmailPayload payload =
                new NotificationEmailService.PromotionCreatedEmailPayload(
                        promotion.getCode(),
                        promotion.getName(),
                        promotion.getPromotionType(),
                        promotion.getDiscountValue(),
                        promotion.getStartDate(),
                        promotion.getEndDate(),
                        promotion.getStatus(),
                        normalizeValue(promotion.getDescription()),
                        resolveCustomerGroups(promotion)
                );
        List<Recipient> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        executeAfterCommit(() -> recipients.forEach(recipient ->
                notificationEmailService.sendPromotionCreatedEmail(recipient.email(), recipient.name(), payload)
        ));
    }

    private List<String> resolveCustomerGroups(PromotionEntity promotion) {
        if (promotion.getCustomerGroups() == null) {
            return List.of();
        }
        return promotion.getCustomerGroups().stream()
                .filter(scope -> scope.getDeletedAt() == null)
                .map(PromotionCustomerGroupEntity::getCustomerGroup)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<Recipient> resolveRecipients() {
        Map<String, Recipient> recipientsByEmail = new LinkedHashMap<>();
        customerProfileRepository.findAllWithUser().forEach(customer ->
                addRecipient(recipientsByEmail, resolveEmail(customer), resolveName(customer))
        );
        return recipientsByEmail.values().stream().toList();
    }

    private String resolveEmail(CustomerProfileEntity customer) {
        if (StringUtils.hasText(customer.getEmail())) {
            return customer.getEmail().trim();
        }
        if (customer.getUser() != null && StringUtils.hasText(customer.getUser().getEmail())) {
            return customer.getUser().getEmail().trim();
        }
        return null;
    }

    private String resolveName(CustomerProfileEntity customer) {
        if (StringUtils.hasText(customer.getContactPerson())) {
            return customer.getContactPerson().trim();
        }
        if (StringUtils.hasText(customer.getCompanyName())) {
            return customer.getCompanyName().trim();
        }
        if (customer.getUser() != null && StringUtils.hasText(customer.getUser().getFullName())) {
            return customer.getUser().getFullName().trim();
        }
        return null;
    }

    private void addRecipient(Map<String, Recipient> recipientsByEmail, String email, String name) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        recipientsByEmail.putIfAbsent(normalizedEmail, new Recipient(normalizedEmail, normalizeValue(name)));
    }

    private String normalizeValue(String value) {
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
