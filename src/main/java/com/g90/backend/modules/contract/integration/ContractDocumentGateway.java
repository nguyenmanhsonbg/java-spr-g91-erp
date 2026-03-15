package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.contract.entity.ContractDocumentType;
import com.g90.backend.modules.contract.entity.ContractEntity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

public interface ContractDocumentGateway {

    DocumentDescriptor generatePreview(ContractEntity contract, ContractDocumentType documentType, boolean officialDocument, long sequence);

    record DocumentDescriptor(
            String fileName,
            String fileUrl,
            String documentNumber,
            String watermarkText
    ) {
    }
}

@Component
class DefaultContractDocumentGateway implements ContractDocumentGateway {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public DocumentDescriptor generatePreview(ContractEntity contract, ContractDocumentType documentType, boolean officialDocument, long sequence) {
        LocalDate today = LocalDate.now(APP_ZONE);
        String prefix = switch (documentType) {
            case SALES_CONTRACT -> "SC";
            case PROFORMA_INVOICE -> "PI";
            case DELIVERY_NOTE -> "DN";
            case PACKING_LIST -> "PL";
        };
        String documentNumber = officialDocument
                ? prefix + "-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence)
                : null;
        String fileName = prefix + "-" + contract.getContractNumber() + ".pdf";
        String fileUrl = "/generated/contracts/" + contract.getId() + "/" + fileName;
        return new DocumentDescriptor(
                fileName,
                fileUrl,
                documentNumber,
                officialDocument ? null : "DRAFT"
        );
    }
}
