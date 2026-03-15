package com.g90.backend.modules.contract.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
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
import com.g90.backend.modules.contract.service.ContractService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ContractController {

    private final ContractService contractService;

    @GetMapping("/form-init")
    public ApiResponse<ContractFormInitResponseData> getContractFormInit(@Valid @ModelAttribute ContractFormInitQuery query) {
        return ApiResponse.success("Contract form data loaded successfully", contractService.getContractFormInit(query));
    }

    @PostMapping("/preview")
    public ApiResponse<ContractPreviewResponseData> previewContract(@Valid @RequestBody ContractPreviewRequest request) {
        return ApiResponse.success("Contract preview calculated successfully", contractService.previewContract(request));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContractDetailResponseData>> createContract(@Valid @RequestBody ContractCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contract created successfully", contractService.createContract(request)));
    }

    @PostMapping("/from-quotation/{quotationId}")
    public ApiResponse<ContractFromQuotationResponseData> createFromQuotation(
            @PathVariable String quotationId,
            @Valid @RequestBody CreateContractFromQuotationRequest request
    ) {
        return ApiResponse.success("Contract created from quotation successfully", contractService.createFromQuotation(quotationId, request));
    }

    @GetMapping
    public ApiResponse<ContractListResponseData> getContracts(@Valid @ModelAttribute ContractListQuery query) {
        return ApiResponse.success("Contract list fetched successfully", contractService.getContracts(query));
    }

    @GetMapping("/approvals/pending")
    public ApiResponse<PendingContractApprovalListResponseData> getPendingApprovals(@Valid @ModelAttribute PendingContractApprovalListQuery query) {
        return ApiResponse.success("Pending approvals fetched successfully", contractService.getPendingApprovals(query));
    }

    @GetMapping("/{contractId}/approval-review")
    public ApiResponse<ContractApprovalReviewResponseData> getApprovalReview(@PathVariable String contractId) {
        return ApiResponse.success("Contract approval review loaded successfully", contractService.getApprovalReview(contractId));
    }

    @GetMapping("/{contractId}")
    public ApiResponse<ContractDetailResponseData> getContractDetail(@PathVariable String contractId) {
        return ApiResponse.success("Contract detail fetched successfully", contractService.getContractDetail(contractId));
    }

    @PutMapping("/{contractId}")
    public ApiResponse<ContractDetailResponseData> updateContract(
            @PathVariable String contractId,
            @Valid @RequestBody ContractUpdateRequest request
    ) {
        return ApiResponse.success("Contract updated successfully", contractService.updateContract(contractId, request));
    }

    @PostMapping("/{contractId}/cancel")
    public ApiResponse<ContractApprovalResponseData> cancelContract(
            @PathVariable String contractId,
            @Valid @RequestBody ContractCancelRequest request
    ) {
        return ApiResponse.success("Contract cancellation processed successfully", contractService.cancelContract(contractId, request));
    }

    @PostMapping("/{contractId}/submit")
    public ApiResponse<ContractApprovalResponseData> submitContract(
            @PathVariable String contractId,
            @Valid @RequestBody ContractSubmitRequest request
    ) {
        return ApiResponse.success("Contract submission processed successfully", contractService.submitContract(contractId, request));
    }

    @GetMapping("/{contractId}/tracking")
    public ApiResponse<ContractTrackingResponseData> getTracking(@PathVariable String contractId) {
        return ApiResponse.success("Contract tracking fetched successfully", contractService.getTracking(contractId));
    }

    @GetMapping("/{contractId}/documents")
    public ApiResponse<ContractDocumentListResponseData> getDocuments(@PathVariable String contractId) {
        return ApiResponse.success("Contract documents fetched successfully", contractService.getDocuments(contractId));
    }

    @PostMapping("/{contractId}/documents/generate")
    public ResponseEntity<ApiResponse<ContractDetailResponseData.DocumentData>> generateDocument(
            @PathVariable String contractId,
            @Valid @RequestBody ContractDocumentGenerateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contract document generated successfully", contractService.generateDocument(contractId, request)));
    }

    @PostMapping("/{contractId}/documents/{documentId}/export")
    public ApiResponse<ContractDetailResponseData.DocumentData> exportDocument(
            @PathVariable String contractId,
            @PathVariable String documentId,
            @Valid @RequestBody ContractDocumentExportRequest request
    ) {
        return ApiResponse.success("Contract document exported successfully", contractService.exportDocument(contractId, documentId, request));
    }

    @PostMapping("/{contractId}/documents/{documentId}/email")
    public ApiResponse<ContractDetailResponseData.DocumentData> emailDocument(
            @PathVariable String contractId,
            @PathVariable String documentId,
            @Valid @RequestBody ContractDocumentEmailRequest request
    ) {
        return ApiResponse.success("Contract document emailed successfully", contractService.emailDocument(contractId, documentId, request));
    }

    @PostMapping("/{contractId}/approve")
    public ApiResponse<ContractApprovalResponseData> approveContract(
            @PathVariable String contractId,
            @Valid @RequestBody ContractApprovalDecisionRequest request
    ) {
        return ApiResponse.success("Contract approval processed successfully", contractService.approveContract(contractId, request));
    }

    @PostMapping("/{contractId}/reject")
    public ApiResponse<ContractApprovalResponseData> rejectContract(
            @PathVariable String contractId,
            @Valid @RequestBody ContractApprovalDecisionRequest request
    ) {
        return ApiResponse.success("Contract rejection processed successfully", contractService.rejectContract(contractId, request));
    }

    @PostMapping("/{contractId}/request-modification")
    public ApiResponse<ContractApprovalResponseData> requestModification(
            @PathVariable String contractId,
            @Valid @RequestBody ContractApprovalDecisionRequest request
    ) {
        return ApiResponse.success("Contract modification request processed successfully", contractService.requestModification(contractId, request));
    }
}
