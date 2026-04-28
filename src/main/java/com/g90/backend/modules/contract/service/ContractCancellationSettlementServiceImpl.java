package com.g90.backend.modules.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ContractCancellationSettlementNotFoundException;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementConfirmRequest;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementListResponseData;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementQuery;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementResponseData;
import com.g90.backend.modules.contract.entity.ContractCancellationSettlementEntity;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.repository.ContractCancellationSettlementRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContractCancellationSettlementServiceImpl implements ContractCancellationSettlementService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String COMPANY_REFUND_TYPE = "COMPANY_CANCEL_REFUND_AND_COMPENSATION";
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("CASH", "BANK_TRANSFER", "OTHER");
    private static final Set<String> ALLOWED_QUERY_STATUSES = Set.of("PENDING", "PAID", "SETTLED", "CANCELLED");

    private final ContractCancellationSettlementRepository settlementRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ContractCancellationSettlementServiceImpl(
            ContractCancellationSettlementRepository settlementRepository,
            CustomerProfileRepository customerProfileRepository,
            CurrentUserProvider currentUserProvider,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper
    ) {
        this.settlementRepository = settlementRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ContractCancellationSettlementListResponseData getSettlements(ContractCancellationSettlementQuery query) {
        AuthenticatedUser currentUser = requireViewer();
        normalizeAndValidateQuery(query);

        List<ContractCancellationSettlementEntity> base = loadAccessibleSettlements(currentUser, query);
        List<ContractCancellationSettlementEntity> matched = base.stream()
                .filter(settlement -> matchesQuery(settlement, query))
                .toList();

        int page = query.getPage();
        int pageSize = query.getPageSize();
        int fromIndex = Math.min((page - 1) * pageSize, matched.size());
        int toIndex = Math.min(fromIndex + pageSize, matched.size());
        int totalPages = matched.isEmpty() ? 0 : (int) Math.ceil((double) matched.size() / pageSize);

        return new ContractCancellationSettlementListResponseData(
                matched.subList(fromIndex, toIndex).stream().map(this::toListItem).toList(),
                PaginationResponse.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(matched.size())
                        .totalPages(totalPages)
                        .build(),
                new ContractCancellationSettlementListResponseData.Filters(
                        query.getContractId(),
                        query.getCustomerId(),
                        query.getSettlementType(),
                        query.getStatus()
                )
        );
    }

    @Override
    @Transactional
    public ContractCancellationSettlementResponseData getSettlement(String settlementId) {
        AuthenticatedUser currentUser = requireViewer();
        ContractCancellationSettlementEntity settlement = loadAccessibleSettlement(settlementId, currentUser);
        return toResponse(settlement);
    }

    @Override
    @Transactional
    public ContractCancellationSettlementResponseData confirmRefund(
            String settlementId,
            ContractCancellationSettlementConfirmRequest request
    ) {
        AuthenticatedUser currentUser = requireFinanceOperator();
        normalizeAndValidateConfirmRequest(request);

        ContractCancellationSettlementEntity settlement = settlementRepository.findDetailedById(settlementId)
                .orElseThrow(ContractCancellationSettlementNotFoundException::new);
        ContractCancellationSettlementResponseData oldState = toResponse(settlement);
        validateRefundConfirmable(settlement, request);

        settlement.setPaidAmount(request.getPaidAmount());
        settlement.setPaymentMethod(request.getPaymentMethod());
        settlement.setReferenceNo(request.getReferenceNo());
        settlement.setProofDocumentUrl(request.getProofDocumentUrl());
        settlement.setPaymentNote(request.getNote());
        settlement.setPaidBy(currentUser.userId());
        settlement.setPaidAt(request.getPaidAt() == null ? LocalDateTime.now(APP_ZONE) : request.getPaidAt());
        settlement.setUpdatedBy(currentUser.userId());
        settlement.setStatus("PAID");

        ContractCancellationSettlementEntity saved = settlementRepository.save(settlement);
        ContractCancellationSettlementResponseData response = toResponse(saved);
        logAudit("CONFIRM_CONTRACT_CANCELLATION_REFUND", saved.getId(), oldState, response, currentUser.userId());
        return response;
    }

    private List<ContractCancellationSettlementEntity> loadAccessibleSettlements(
            AuthenticatedUser currentUser,
            ContractCancellationSettlementQuery query
    ) {
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity customer = loadCurrentCustomer(currentUser);
            if (StringUtils.hasText(query.getCustomerId()) && !customer.getId().equals(query.getCustomerId())) {
                return List.of();
            }
            query.setCustomerId(customer.getId());
            return settlementRepository.findByCustomerIdDetailed(customer.getId());
        }
        return settlementRepository.findAllDetailed();
    }

    private ContractCancellationSettlementEntity loadAccessibleSettlement(String settlementId, AuthenticatedUser currentUser) {
        ContractCancellationSettlementEntity settlement = settlementRepository.findDetailedById(normalizeRequired(settlementId, "settlementId", "settlementId is required"))
                .orElseThrow(ContractCancellationSettlementNotFoundException::new);
        if (roleOf(currentUser) == RoleName.CUSTOMER) {
            CustomerProfileEntity customer = loadCurrentCustomer(currentUser);
            if (settlement.getCustomer() == null || !customer.getId().equals(settlement.getCustomer().getId())) {
                throw new ContractCancellationSettlementNotFoundException();
            }
        }
        return settlement;
    }

    private void validateRefundConfirmable(
            ContractCancellationSettlementEntity settlement,
            ContractCancellationSettlementConfirmRequest request
    ) {
        if (!COMPANY_REFUND_TYPE.equals(normalizeUpper(settlement.getSettlementType()))) {
            throw RequestValidationException.singleError("settlementId", "Only company refund settlements can be confirmed as paid");
        }
        if (!"PENDING".equals(normalizeUpper(settlement.getStatus()))) {
            throw RequestValidationException.singleError("status", "Only pending refund settlements can be confirmed");
        }
        BigDecimal totalPayableAmount = normalizeMoney(settlement.getTotalPayableAmount());
        if (totalPayableAmount.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError("totalPayableAmount", "Settlement has no payable amount");
        }
        if (request.getPaidAmount().compareTo(totalPayableAmount) != 0) {
            throw RequestValidationException.singleError("paidAmount", "paidAmount must equal totalPayableAmount");
        }
    }

    private boolean matchesQuery(ContractCancellationSettlementEntity settlement, ContractCancellationSettlementQuery query) {
        ContractEntity contract = settlement.getContract();
        CustomerProfileEntity customer = settlement.getCustomer();
        if (StringUtils.hasText(query.getContractId()) && (contract == null || !query.getContractId().equalsIgnoreCase(contract.getId()))) {
            return false;
        }
        if (StringUtils.hasText(query.getCustomerId()) && (customer == null || !query.getCustomerId().equalsIgnoreCase(customer.getId()))) {
            return false;
        }
        if (StringUtils.hasText(query.getSettlementType()) && !query.getSettlementType().equalsIgnoreCase(settlement.getSettlementType())) {
            return false;
        }
        return !StringUtils.hasText(query.getStatus()) || query.getStatus().equalsIgnoreCase(settlement.getStatus());
    }

    private ContractCancellationSettlementListResponseData.Item toListItem(ContractCancellationSettlementEntity settlement) {
        ContractEntity contract = settlement.getContract();
        CustomerProfileEntity customer = settlement.getCustomer();
        return new ContractCancellationSettlementListResponseData.Item(
                settlement.getId(),
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getContractNumber(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCustomerCode(),
                customer == null ? null : customer.getCompanyName(),
                normalizeUpper(settlement.getSettlementType()),
                normalizeMoney(settlement.getTotalPayableAmount()),
                normalizeMoney(settlement.getPaidAmount()),
                normalizeUpper(settlement.getStatus()),
                settlement.getCreatedAt(),
                settlement.getPaidAt()
        );
    }

    private ContractCancellationSettlementResponseData toResponse(ContractCancellationSettlementEntity settlement) {
        ContractEntity contract = settlement.getContract();
        CustomerProfileEntity customer = settlement.getCustomer();
        return new ContractCancellationSettlementResponseData(
                settlement.getId(),
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getContractNumber(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCustomerCode(),
                customer == null ? null : customer.getCompanyName(),
                normalizeUpper(settlement.getSettlementType()),
                normalizeMoney(settlement.getDepositRefundAmount()),
                normalizeMoney(settlement.getCompensationAmount()),
                normalizeMoney(settlement.getForfeitedDepositAmount()),
                normalizeMoney(settlement.getTotalPayableAmount()),
                normalizeMoney(settlement.getPaidAmount()),
                normalizeUpper(settlement.getStatus()),
                normalizeUpper(settlement.getPaymentMethod()),
                normalizeNullable(settlement.getReferenceNo()),
                normalizeNullable(settlement.getProofDocumentUrl()),
                normalizeNullable(settlement.getNote()),
                normalizeNullable(settlement.getPaymentNote()),
                settlement.getCreatedBy(),
                settlement.getUpdatedBy(),
                settlement.getPaidBy(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt(),
                settlement.getPaidAt()
        );
    }

    private void normalizeAndValidateQuery(ContractCancellationSettlementQuery query) {
        query.setContractId(normalizeNullable(query.getContractId()));
        query.setCustomerId(normalizeNullable(query.getCustomerId()));
        query.setSettlementType(StringUtils.hasText(query.getSettlementType()) ? normalizeUpper(query.getSettlementType()) : null);
        query.setStatus(StringUtils.hasText(query.getStatus()) ? normalizeUpper(query.getStatus()) : null);
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        if (StringUtils.hasText(query.getStatus()) && !ALLOWED_QUERY_STATUSES.contains(query.getStatus())) {
            throw RequestValidationException.singleError("status", "status must be one of PENDING, PAID, SETTLED, CANCELLED");
        }
    }

    private void normalizeAndValidateConfirmRequest(ContractCancellationSettlementConfirmRequest request) {
        request.setPaidAmount(normalizePositiveMoney(request.getPaidAmount(), "paidAmount", "paidAmount must be greater than 0"));
        request.setPaymentMethod(resolvePaymentMethod(request.getPaymentMethod()));
        request.setReferenceNo(normalizeNullable(request.getReferenceNo()));
        request.setProofDocumentUrl(normalizeNullable(request.getProofDocumentUrl()));
        request.setNote(normalizeNullable(request.getNote()));
        if ("BANK_TRANSFER".equals(request.getPaymentMethod()) && !StringUtils.hasText(request.getReferenceNo())) {
            throw RequestValidationException.singleError("referenceNo", "referenceNo is required for bank transfer refunds");
        }
    }

    private AuthenticatedUser requireViewer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER && role != RoleName.CUSTOMER) {
            throw new ForbiddenOperationException("You do not have permission to view cancellation settlements");
        }
        return currentUser;
    }

    private AuthenticatedUser requireFinanceOperator() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("Only accountant or owner users can confirm cancellation refunds");
        }
        return currentUser;
    }

    private CustomerProfileEntity loadCurrentCustomer(AuthenticatedUser currentUser) {
        return customerProfileRepository.findByUser_Id(currentUser.userId())
                .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private RoleName roleOf(AuthenticatedUser currentUser) {
        return RoleName.from(currentUser.role());
    }

    private String resolvePaymentMethod(String value) {
        String normalized = normalizeRequired(value, "paymentMethod", "paymentMethod is required").toUpperCase(Locale.ROOT);
        if ("BANK".equals(normalized)) {
            normalized = "BANK_TRANSFER";
        }
        if (!ALLOWED_PAYMENT_METHODS.contains(normalized)) {
            throw RequestValidationException.singleError("paymentMethod", "paymentMethod must be one of CASH, BANK_TRANSFER, OTHER");
        }
        return normalized;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositiveMoney(BigDecimal value, String field, String message) {
        BigDecimal normalized = normalizeMoney(value);
        if (normalized.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError(field, message);
        }
        return normalized;
    }

    private String normalizeRequired(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw RequestValidationException.singleError(field, message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("CONTRACT_CANCELLATION_SETTLEMENT");
        auditLog.setEntityId(entityId);
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
