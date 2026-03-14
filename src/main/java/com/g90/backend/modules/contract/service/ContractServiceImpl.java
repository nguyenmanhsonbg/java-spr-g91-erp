package com.g90.backend.modules.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ContractCreationNotAllowedException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.QuotationAlreadyConvertedException;
import com.g90.backend.exception.QuotationNotFoundException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.dto.ContractFromQuotationResponseData;
import com.g90.backend.modules.contract.dto.CreateContractFromQuotationRequest;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import com.g90.backend.modules.quotation.entity.QuotationStatus;
import com.g90.backend.modules.quotation.repository.QuotationRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ContractRepository contractRepository;
    private final QuotationRepository quotationRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ContractFromQuotationResponseData createFromQuotation(String quotationId, CreateContractFromQuotationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.ACCOUNTANT.name().equalsIgnoreCase(currentUser.role())
                && !RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }

        QuotationEntity quotation = quotationRepository.findDetailedById(quotationId)
                .orElseThrow(QuotationNotFoundException::new);

        if (contractRepository.existsByQuotation_Id(quotationId)
                || QuotationStatus.CONVERTED.name().equalsIgnoreCase(quotation.getStatus())) {
            throw new QuotationAlreadyConvertedException();
        }
        if (QuotationStatus.DRAFT.name().equalsIgnoreCase(quotation.getStatus())
                || QuotationStatus.REJECTED.name().equalsIgnoreCase(quotation.getStatus())) {
            throw new ContractCreationNotAllowedException();
        }

        ContractEntity contract = new ContractEntity();
        contract.setContractNumber(generateContractNumber());
        contract.setCustomer(quotation.getCustomer());
        contract.setQuotation(quotation);
        contract.setTotalAmount(quotation.getTotalAmount());
        contract.setStatus("DRAFT");
        contract.setPaymentTerms(request.getPaymentTerms().trim());
        contract.setDeliveryAddress(request.getDeliveryAddress().trim());
        contract.setCreatedBy(currentUser.userId());

        for (QuotationItemEntity quotationItem : quotation.getItems()) {
            ContractItemEntity contractItem = new ContractItemEntity();
            contractItem.setContract(contract);
            contractItem.setProduct(quotationItem.getProduct());
            contractItem.setQuantity(quotationItem.getQuantity());
            contractItem.setUnitPrice(quotationItem.getUnitPrice());
            contractItem.setTotalPrice(quotationItem.getTotalPrice());
            contract.getItems().add(contractItem);
        }

        ContractEntity savedContract = contractRepository.save(contract);

        String oldStatus = quotation.getStatus();
        quotation.setStatus(QuotationStatus.CONVERTED.name());
        quotationRepository.save(quotation);

        ContractFromQuotationResponseData response = new ContractFromQuotationResponseData(
                new ContractFromQuotationResponseData.ContractData(
                        savedContract.getId(),
                        savedContract.getContractNumber(),
                        savedContract.getCustomer().getId(),
                        quotation.getId(),
                        savedContract.getTotalAmount(),
                        savedContract.getStatus(),
                        savedContract.getPaymentTerms(),
                        savedContract.getDeliveryAddress(),
                        savedContract.getCreatedAt()
                ),
                new ContractFromQuotationResponseData.QuotationData(
                        quotation.getId(),
                        quotation.getQuotationNumber(),
                        quotation.getStatus()
                )
        );

        logQuotationAudit(quotation.getId(), oldStatus, response, currentUser.userId());
        return response;
    }

    private String generateContractNumber() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long sequence = contractRepository.countByCreatedAtBetween(startOfDay, endOfDay) + 1;
        return "CT-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
    }

    private void logQuotationAudit(String quotationId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction("CREATE_CONTRACT_FROM_QUOTATION");
        auditLog.setEntityType("QUOTATION");
        auditLog.setEntityId(quotationId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit payload", exception);
        }
    }
}
