package com.g90.backend.modules.contract.mapper;

import com.g90.backend.modules.contract.dto.ContractDetailResponseData;
import com.g90.backend.modules.contract.dto.ContractItemResponse;
import com.g90.backend.modules.contract.dto.ContractListResponseData;
import com.g90.backend.modules.contract.dto.ContractResponse;
import com.g90.backend.modules.contract.dto.ContractTrackingResponseData;
import com.g90.backend.modules.contract.entity.ContractDocumentEntity;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractItemEntity;
import com.g90.backend.modules.contract.entity.ContractStatusHistoryEntity;
import com.g90.backend.modules.contract.entity.ContractTrackingEventEntity;
import com.g90.backend.modules.contract.entity.ContractVersionEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toContractResponse(ContractEntity contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getContractNumber(),
                contract.getCustomer() == null ? null : contract.getCustomer().getId(),
                contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                contract.getQuotation() == null ? null : contract.getQuotation().getId(),
                contract.getStatus(),
                contract.getApprovalStatus(),
                contract.getApprovalTier(),
                contract.isRequiresApproval(),
                contract.isConfidential(),
                contract.getPaymentTerms(),
                contract.getDeliveryAddress(),
                contract.getDeliveryTerms(),
                contract.getExpectedDeliveryDate(),
                contract.getNote(),
                contract.getTotalAmount(),
                contract.getCreditLimitSnapshot(),
                contract.getCurrentDebtSnapshot(),
                contract.getDepositPercentage(),
                contract.getDepositAmount(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getSubmittedAt(),
                contract.getApprovedAt(),
                contract.getCancelledAt(),
                contract.getAutoSubmitDueAt()
        );
    }

    public ContractItemResponse toItemResponse(ContractItemEntity item) {
        return new ContractItemResponse(
                item.getId(),
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProduct() == null ? null : item.getProduct().getProductCode(),
                item.getProduct() == null ? null : item.getProduct().getProductName(),
                item.getProduct() == null ? null : item.getProduct().getType(),
                item.getProduct() == null ? null : item.getProduct().getSize(),
                item.getProduct() == null ? null : item.getProduct().getThickness(),
                item.getProduct() == null ? null : item.getProduct().getUnit(),
                item.getQuantity(),
                item.getBaseUnitPrice(),
                item.getUnitPrice(),
                item.getDiscountAmount(),
                item.getTotalPrice(),
                item.getPriceOverrideReason()
        );
    }

    public List<ContractItemResponse> toItemResponses(List<ContractItemEntity> items) {
        return items.stream().map(this::toItemResponse).toList();
    }

    public ContractListResponseData.Item toListItem(ContractEntity contract) {
        return new ContractListResponseData.Item(
                contract.getId(),
                contract.getContractNumber(),
                contract.getCustomer() == null ? null : contract.getCustomer().getId(),
                contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                contract.getStatus(),
                contract.getApprovalStatus(),
                contract.isConfidential(),
                contract.getTotalAmount(),
                contract.getExpectedDeliveryDate(),
                contract.getSubmittedAt(),
                contract.getCreatedAt()
        );
    }

    public ContractDetailResponseData.DocumentData toDocumentData(ContractDocumentEntity document) {
        return new ContractDetailResponseData.DocumentData(
                document.getId(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getFileName(),
                document.getFileUrl(),
                document.isPreviewOnly(),
                document.isOfficialDocument(),
                document.getWatermarkText(),
                document.getExportCount(),
                document.getGeneratedAt(),
                document.getLastExportedAt(),
                document.getEmailedAt()
        );
    }

    public ContractDetailResponseData.VersionData toVersionData(ContractVersionEntity version) {
        return new ContractDetailResponseData.VersionData(
                version.getVersionNo(),
                version.getChangeReason(),
                version.getChangedBy(),
                version.getCreatedAt()
        );
    }

    public ContractDetailResponseData.StatusHistoryData toStatusHistoryData(ContractStatusHistoryEntity statusHistory) {
        return new ContractDetailResponseData.StatusHistoryData(
                statusHistory.getFromStatus(),
                statusHistory.getToStatus(),
                statusHistory.getChangeReason(),
                statusHistory.getChangedBy(),
                statusHistory.getChangedAt()
        );
    }

    public ContractTrackingResponseData.Event toTrackingEvent(ContractTrackingEventEntity event) {
        return new ContractTrackingResponseData.Event(
                event.getEventType(),
                event.getEventStatus(),
                event.getTitle(),
                event.getNote(),
                event.getExpectedAt(),
                event.getActualAt(),
                event.getTrackingNumber()
        );
    }
}
