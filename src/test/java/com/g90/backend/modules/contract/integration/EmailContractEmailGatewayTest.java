package com.g90.backend.modules.contract.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.g90.backend.modules.contract.entity.ContractDocumentEntity;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.email.config.EmailProperties;
import com.g90.backend.modules.email.service.NotificationEmailService;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailContractEmailGatewayTest {

    @Mock
    private NotificationEmailService notificationEmailService;

    @Test
    void sendDocumentBuildsDocumentEmailPayload() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.setAssetsBaseUrl("https://portal.g90steel.vn");
        EmailContractEmailGateway gateway = new EmailContractEmailGateway(notificationEmailService, emailProperties);

        ContractEntity contract = new ContractEntity();
        contract.setId("contract-1");
        contract.setContractNumber("CT-20260322-0001");
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setCompanyName("Customer A");
        customer.setContactPerson("Nguyen Van A");
        contract.setCustomer(customer);

        ContractDocumentEntity document = new ContractDocumentEntity();
        document.setId("document-1");
        document.setContract(contract);
        document.setDocumentType("SALES_CONTRACT");
        document.setDocumentNumber("SC-20260322-0001");
        document.setFileName("SC-CT-20260322-0001.pdf");
        document.setFileUrl("/generated/contracts/contract-1/SC-CT-20260322-0001.pdf");

        var result = gateway.sendDocument(document, "recipient@example.com");

        assertThat(result.accepted()).isTrue();
        ArgumentCaptor<NotificationEmailService.ContractDocumentEmailPayload> payloadCaptor =
                ArgumentCaptor.forClass(NotificationEmailService.ContractDocumentEmailPayload.class);
        verify(notificationEmailService).sendContractDocumentEmail(
                org.mockito.ArgumentMatchers.eq("recipient@example.com"),
                org.mockito.ArgumentMatchers.eq("Nguyen Van A"),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue().documentUrl())
                .isEqualTo("https://portal.g90steel.vn/generated/contracts/contract-1/SC-CT-20260322-0001.pdf");
    }
}
