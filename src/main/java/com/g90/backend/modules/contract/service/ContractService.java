package com.g90.backend.modules.contract.service;

import com.g90.backend.modules.contract.dto.ContractApprovalDecisionRequest;
import com.g90.backend.modules.contract.dto.ContractApprovalReviewResponseData;
import com.g90.backend.modules.contract.dto.ContractApprovalResponseData;
import com.g90.backend.modules.contract.dto.ContractCancelRequest;
import com.g90.backend.modules.contract.dto.ContractCreateRequest;
import com.g90.backend.modules.contract.dto.ContractDetailResponseData;
import com.g90.backend.modules.contract.dto.ContractFormInitQuery;
import com.g90.backend.modules.contract.dto.ContractFormInitResponseData;
import com.g90.backend.modules.contract.dto.ContractDocumentEmailRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentExportRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentGenerateRequest;
import com.g90.backend.modules.contract.dto.ContractDocumentListResponseData;
import com.g90.backend.modules.contract.dto.ContractFromQuotationResponseData;
import com.g90.backend.modules.contract.dto.ContractListQuery;
import com.g90.backend.modules.contract.dto.ContractListResponseData;
import com.g90.backend.modules.contract.dto.ContractPreviewRequest;
import com.g90.backend.modules.contract.dto.ContractPreviewResponseData;
import com.g90.backend.modules.contract.dto.ContractSubmitRequest;
import com.g90.backend.modules.contract.dto.ContractTrackingResponseData;
import com.g90.backend.modules.contract.dto.ContractUpdateRequest;
import com.g90.backend.modules.contract.dto.CreateContractFromQuotationRequest;
import com.g90.backend.modules.contract.dto.PendingContractApprovalListQuery;
import com.g90.backend.modules.contract.dto.PendingContractApprovalListResponseData;

public interface ContractService {

    ContractFormInitResponseData getContractFormInit(ContractFormInitQuery query);

    ContractPreviewResponseData previewContract(ContractPreviewRequest request);

    ContractDetailResponseData createContract(ContractCreateRequest request);

    ContractFromQuotationResponseData createFromQuotation(String quotationId, CreateContractFromQuotationRequest request);

    ContractListResponseData getContracts(ContractListQuery query);

    ContractDetailResponseData getContractDetail(String contractId);

    ContractDetailResponseData updateContract(String contractId, ContractUpdateRequest request);

    ContractApprovalResponseData cancelContract(String contractId, ContractCancelRequest request);

    ContractApprovalResponseData submitContract(String contractId, ContractSubmitRequest request);

    ContractApprovalResponseData approveByCustomer(String contractId, ContractApprovalDecisionRequest request);

    ContractApprovalResponseData rejectByCustomer(String contractId, ContractApprovalDecisionRequest request);

    ContractApprovalResponseData rejectCustomerApproval(String contractId, ContractApprovalDecisionRequest request);

    ContractTrackingResponseData getTracking(String contractId);

    ContractDocumentListResponseData getDocuments(String contractId);

    ContractDetailResponseData.DocumentData generateDocument(String contractId, ContractDocumentGenerateRequest request);

    ContractDetailResponseData.DocumentData exportDocument(String contractId, String documentId, ContractDocumentExportRequest request);

    ContractDetailResponseData.DocumentData emailDocument(String contractId, String documentId, ContractDocumentEmailRequest request);

    PendingContractApprovalListResponseData getPendingApprovals(PendingContractApprovalListQuery query);

    ContractApprovalReviewResponseData getApprovalReview(String contractId);

    ContractApprovalResponseData approveContract(String contractId, ContractApprovalDecisionRequest request);

    ContractApprovalResponseData rejectContract(String contractId, ContractApprovalDecisionRequest request);

    ContractApprovalResponseData requestModification(String contractId, ContractApprovalDecisionRequest request);
}
