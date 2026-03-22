package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.contract.entity.ContractDocumentEntity;
import com.g90.backend.modules.email.config.EmailProperties;
import com.g90.backend.modules.email.service.NotificationEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailContractEmailGateway implements ContractEmailGateway {

    private final NotificationEmailService notificationEmailService;
    private final EmailProperties emailProperties;

    @Override
    public EmailResult sendDocument(ContractDocumentEntity document, String recipientEmail) {
        NotificationEmailService.ContractDocumentEmailPayload payload =
                new NotificationEmailService.ContractDocumentEmailPayload(
                        document.getContract().getContractNumber(),
                        document.getContract().getCustomer() == null ? null : document.getContract().getCustomer().getCompanyName(),
                        humanize(document.getDocumentType()),
                        document.getDocumentNumber(),
                        document.getFileName(),
                        buildPublicUrl(document.getFileUrl())
                );

        executeAfterCommit(() -> notificationEmailService.sendContractDocumentEmail(
                recipientEmail.trim(),
                document.getContract().getCustomer() == null ? null : document.getContract().getCustomer().getContactPerson(),
                payload
        ));

        log.info("Queued contract document email contract={} document={} recipient={}",
                document.getContract().getContractNumber(),
                document.getId(),
                recipientEmail);
        return new EmailResult(true, "Contract document email queued");
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

    private String buildPublicUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return fileUrl;
        }
        if (!StringUtils.hasText(emailProperties.getAssetsBaseUrl())) {
            return fileUrl;
        }
        String baseUrl = emailProperties.getAssetsBaseUrl().endsWith("/")
                ? emailProperties.getAssetsBaseUrl().substring(0, emailProperties.getAssetsBaseUrl().length() - 1)
                : emailProperties.getAssetsBaseUrl();
        String relativePath = fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl;
        return baseUrl + relativePath;
    }

    private String humanize(String value) {
        if (!StringUtils.hasText(value)) {
            return "Contract document";
        }
        String normalized = value.trim().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
