package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.contract.entity.ContractDocumentEntity;

public interface ContractEmailGateway {

    EmailResult sendDocument(ContractDocumentEntity document, String recipientEmail);

    record EmailResult(boolean accepted, String message) {
    }
}

class NoopContractEmailGateway implements ContractEmailGateway {

    @Override
    public EmailResult sendDocument(ContractDocumentEntity document, String recipientEmail) {
        return new EmailResult(true, "Email dispatch queued via placeholder gateway");
    }
}
