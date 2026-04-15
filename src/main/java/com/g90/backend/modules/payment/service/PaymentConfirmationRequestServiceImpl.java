package com.g90.backend.modules.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InvoiceNotFoundException;
import com.g90.backend.exception.PaymentConfirmationRequestNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.debt.entity.PaymentEntity;
import com.g90.backend.modules.debt.service.PaymentExecutionCommand;
import com.g90.backend.modules.debt.service.PaymentExecutionService;
import com.g90.backend.modules.email.service.EmailService;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestConfirmRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestCreateRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListQuery;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListResponseData;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestRejectRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestResponse;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.entity.PaymentConfirmationRequestEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.payment.repository.PaymentConfirmationRequestRepository;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaymentConfirmationRequestServiceImpl implements PaymentConfirmationRequestService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> PAYABLE_INVOICE_STATUSES = Set.of("ISSUED", "PARTIALLY_PAID");
    private static final Set<String> ALLOWED_REQUEST_STATUSES = Set.of("PENDING_REVIEW", "CONFIRMED", "REJECTED", "CANCELLED");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "transferTime", "requestedAmount", "status", "reviewedAt", "invoiceNumber", "customerName");
    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

    private final PaymentConfirmationRequestRepository paymentConfirmationRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final InvoiceService invoiceService;
    private final PaymentExecutionService paymentExecutionService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public PaymentConfirmationRequestServiceImpl(
            PaymentConfirmationRequestRepository paymentConfirmationRequestRepository,
            InvoiceRepository invoiceRepository,
            CustomerProfileRepository customerProfileRepository,
            InvoiceService invoiceService,
            PaymentExecutionService paymentExecutionService,
            CurrentUserProvider currentUserProvider,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            EmailService emailService
    ) {
        this.paymentConfirmationRequestRepository = paymentConfirmationRequestRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.invoiceService = invoiceService;
        this.paymentExecutionService = paymentExecutionService;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public PaymentConfirmationRequestResponse createRequest(String invoiceId, PaymentConfirmationRequestCreateRequest request) {
        AuthenticatedUser currentUser = requireCustomer();
        CustomerProfileEntity customer = loadCurrentCustomer();
        normalizeAndValidateCreateRequest(request);

        InvoiceResponse invoice = invoiceService.getInvoice(invoiceId);
        validateInvoiceCanReceiveConfirmation(invoice, request.getRequestedAmount(), "requestedAmount");
        if (paymentConfirmationRequestRepository.existsByInvoice_IdAndStatus(invoiceId, "PENDING_REVIEW")) {
            throw RequestValidationException.singleError("invoiceId", "Invoice already has a pending payment confirmation request");
        }

        InvoiceEntity invoiceEntity = invoiceRepository.findDetailedById(invoiceId).orElseThrow(InvoiceNotFoundException::new);
        PaymentConfirmationRequestEntity entity = new PaymentConfirmationRequestEntity();
        entity.setInvoice(invoiceEntity);
        entity.setCustomer(customer);
        entity.setRequestedAmount(request.getRequestedAmount());
        entity.setTransferTime(request.getTransferTime());
        entity.setSenderBankName(request.getSenderBankName());
        entity.setSenderAccountName(request.getSenderAccountName());
        entity.setSenderAccountNo(request.getSenderAccountNo());
        entity.setReferenceCode(request.getReferenceCode());
        entity.setProofDocumentUrl(request.getProofDocumentUrl());
        entity.setNote(request.getNote());
        entity.setStatus("PENDING_REVIEW");
        entity.setCreatedBy(currentUser.userId());
        entity.setUpdatedBy(currentUser.userId());

        PaymentConfirmationRequestEntity saved = paymentConfirmationRequestRepository.save(entity);
        PaymentConfirmationRequestResponse response = toResponse(saved, invoice);
        logAudit("CREATE_PAYMENT_CONFIRMATION_REQUEST", "PAYMENT_CONFIRMATION_REQUEST", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public PaymentConfirmationRequestListResponseData listRequests(PaymentConfirmationRequestListQuery query) {
        normalizeAndValidateListQuery(query);
        return toListResponse(loadAccessibleRequests(query), query);
    }

    @Override
    @Transactional
    public PaymentConfirmationRequestResponse getDetail(String requestId) {
        PaymentConfirmationRequestEntity entity = loadAccessibleRequest(requestId);
        return toResponse(entity, invoiceService.getInvoice(entity.getInvoice().getId()));
    }

    @Override
    @Transactional
    public PaymentConfirmationRequestResponse confirmRequest(String requestId, PaymentConfirmationRequestConfirmRequest request) {
        AuthenticatedUser currentUser = requireReviewer();
        normalizeAndValidateConfirmRequest(request);

        PaymentConfirmationRequestEntity entity = paymentConfirmationRequestRepository.findDetailedById(requestId)
                .orElseThrow(PaymentConfirmationRequestNotFoundException::new);
        validatePendingReview(entity);

        InvoiceResponse invoice = invoiceService.getInvoice(entity.getInvoice().getId());
        validateInvoiceCanReceiveConfirmation(invoice, request.getConfirmedAmount(), "confirmedAmount");
        PaymentConfirmationRequestResponse oldState = toResponse(entity, invoice);

        PaymentEntity payment = paymentExecutionService.recordPayment(new PaymentExecutionCommand(
                entity.getCustomer().getId(),
                entity.getTransferTime().toLocalDate(),
                request.getConfirmedAmount(),
                "BANK_TRANSFER",
                entity.getReferenceCode(),
                firstNonBlank(request.getReviewNote(), entity.getNote(), "Payment confirmation request " + entity.getId()),
                entity.getProofDocumentUrl(),
                "CONFIRMED",
                currentUser.userId(),
                List.of(new PaymentExecutionCommand.PaymentExecutionAllocation(
                        entity.getInvoice().getId(),
                        request.getConfirmedAmount()
                ))
        ));

        entity.setStatus("CONFIRMED");
        entity.setConfirmedAmount(request.getConfirmedAmount());
        entity.setPaymentId(payment.getId());
        entity.setReviewNote(request.getReviewNote());
        entity.setReviewedBy(currentUser.userId());
        entity.setReviewedAt(LocalDateTime.now(APP_ZONE));
        entity.setUpdatedBy(currentUser.userId());

        PaymentConfirmationRequestEntity saved = paymentConfirmationRequestRepository.save(entity);
        InvoiceResponse latestInvoice = invoiceService.getInvoice(entity.getInvoice().getId());
        PaymentConfirmationRequestResponse response = toResponse(saved, latestInvoice);

        logAudit("CONFIRM_PAYMENT_CONFIRMATION_REQUEST", "PAYMENT_CONFIRMATION_REQUEST", saved.getId(), oldState, response, currentUser.userId());
        logAudit("CREATE_PAYMENT", "PAYMENT", payment.getId(), null, buildPaymentAuditPayload(payment, saved), currentUser.userId());
        logAudit("ALLOCATE_PAYMENT", "PAYMENT_ALLOCATION", saved.getId(), null, Map.of(
                "paymentId", payment.getId(),
                "invoiceId", entity.getInvoice().getId(),
                "invoiceNumber", entity.getInvoice().getInvoiceNumber(),
                "allocatedAmount", request.getConfirmedAmount()
        ), currentUser.userId());
        maybeSendReviewNotification(saved, latestInvoice, "payment-confirmation-confirmed", "Payment confirmation approved - " + entity.getInvoice().getInvoiceNumber());
        return response;
    }

    @Override
    @Transactional
    public PaymentConfirmationRequestResponse rejectRequest(String requestId, PaymentConfirmationRequestRejectRequest request) {
        AuthenticatedUser currentUser = requireReviewer();
        request.setReason(normalizeRequired(request.getReason(), "reason", "reason is required"));

        PaymentConfirmationRequestEntity entity = paymentConfirmationRequestRepository.findDetailedById(requestId)
                .orElseThrow(PaymentConfirmationRequestNotFoundException::new);
        validatePendingReview(entity);

        InvoiceResponse invoice = invoiceService.getInvoice(entity.getInvoice().getId());
        PaymentConfirmationRequestResponse oldState = toResponse(entity, invoice);

        entity.setStatus("REJECTED");
        entity.setReviewNote(request.getReason());
        entity.setReviewedBy(currentUser.userId());
        entity.setReviewedAt(LocalDateTime.now(APP_ZONE));
        entity.setUpdatedBy(currentUser.userId());

        PaymentConfirmationRequestEntity saved = paymentConfirmationRequestRepository.save(entity);
        PaymentConfirmationRequestResponse response = toResponse(saved, invoice);
        logAudit("REJECT_PAYMENT_CONFIRMATION_REQUEST", "PAYMENT_CONFIRMATION_REQUEST", saved.getId(), oldState, response, currentUser.userId());
        maybeSendReviewNotification(saved, invoice, "payment-confirmation-rejected", "Payment confirmation rejected - " + entity.getInvoice().getInvoiceNumber());
        return response;
    }

    @Override
    @Transactional
    public PaymentConfirmationRequestListResponseData listRequestsByInvoice(String invoiceId, PaymentConfirmationRequestListQuery query) {
        normalizeAndValidateListQuery(query);
        invoiceService.getInvoice(invoiceId);
        query.setInvoiceId(invoiceId);
        return toListResponse(loadAccessibleRequests(query), query);
    }

    private List<PaymentConfirmationRequestEntity> loadAccessibleRequests(PaymentConfirmationRequestListQuery query) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity currentCustomer = loadCurrentCustomer();
            if (StringUtils.hasText(query.getCustomerId()) && !currentCustomer.getId().equalsIgnoreCase(query.getCustomerId())) {
                throw new CustomerProfileNotFoundException();
            }
            query.setCustomerId(currentCustomer.getId());
            return paymentConfirmationRequestRepository.findByCustomerIdWithInvoiceAndCustomer(currentCustomer.getId());
        }
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("You do not have permission to view payment confirmation requests");
        }
        return paymentConfirmationRequestRepository.findAllWithInvoiceAndCustomer();
    }

    private PaymentConfirmationRequestEntity loadAccessibleRequest(String requestId) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role == RoleName.CUSTOMER) {
            CustomerProfileEntity customer = loadCurrentCustomer();
            return paymentConfirmationRequestRepository.findDetailedByIdAndCustomerId(requestId, customer.getId())
                    .orElseThrow(PaymentConfirmationRequestNotFoundException::new);
        }
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("You do not have permission to view payment confirmation requests");
        }
        return paymentConfirmationRequestRepository.findDetailedById(requestId)
                .orElseThrow(PaymentConfirmationRequestNotFoundException::new);
    }

    private PaymentConfirmationRequestListResponseData toListResponse(List<PaymentConfirmationRequestEntity> requests, PaymentConfirmationRequestListQuery query) {
        List<PaymentConfirmationRequestEntity> matched = requests.stream()
                .filter(entity -> matchesQuery(entity, query))
                .sorted(buildComparator(query))
                .toList();
        int page = query.getPage();
        int pageSize = query.getPageSize();
        int fromIndex = Math.min((page - 1) * pageSize, matched.size());
        int toIndex = Math.min(fromIndex + pageSize, matched.size());
        int totalPages = matched.isEmpty() ? 0 : (int) Math.ceil((double) matched.size() / pageSize);
        return new PaymentConfirmationRequestListResponseData(
                matched.subList(fromIndex, toIndex).stream().map(this::toListItem).toList(),
                PaginationResponse.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(matched.size())
                        .totalPages(totalPages)
                        .build(),
                new PaymentConfirmationRequestListResponseData.Filters(
                        query.getKeyword(),
                        query.getInvoiceId(),
                        query.getCustomerId(),
                        query.getStatus()
                )
        );
    }

    private PaymentConfirmationRequestListResponseData.Item toListItem(PaymentConfirmationRequestEntity entity) {
        return new PaymentConfirmationRequestListResponseData.Item(
                entity.getId(),
                entity.getInvoice().getId(),
                entity.getInvoice().getInvoiceNumber(),
                entity.getCustomer().getId(),
                entity.getCustomer().getCustomerCode(),
                entity.getCustomer().getCompanyName(),
                normalizeMoney(entity.getRequestedAmount()),
                normalizeMoney(entity.getConfirmedAmount()),
                entity.getTransferTime(),
                entity.getSenderBankName(),
                entity.getSenderAccountName(),
                entity.getSenderAccountNo(),
                entity.getReferenceCode(),
                entity.getProofDocumentUrl(),
                normalizeUpper(entity.getStatus()),
                entity.getReviewNote(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getPaymentId(),
                entity.getCreatedAt()
        );
    }

    private PaymentConfirmationRequestResponse toResponse(PaymentConfirmationRequestEntity entity, InvoiceResponse invoice) {
        return new PaymentConfirmationRequestResponse(
                entity.getId(),
                entity.getInvoice().getId(),
                entity.getInvoice().getInvoiceNumber(),
                entity.getCustomer().getId(),
                entity.getCustomer().getCustomerCode(),
                entity.getCustomer().getCompanyName(),
                normalizeMoney(entity.getRequestedAmount()),
                normalizeMoney(entity.getConfirmedAmount()),
                entity.getTransferTime(),
                entity.getSenderBankName(),
                entity.getSenderAccountName(),
                entity.getSenderAccountNo(),
                entity.getReferenceCode(),
                entity.getProofDocumentUrl(),
                entity.getNote(),
                normalizeUpper(entity.getStatus()),
                entity.getReviewNote(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPaymentId(),
                invoice.grandTotal(),
                invoice.paidAmount(),
                invoice.outstandingAmount(),
                invoice.status()
        );
    }

    private boolean matchesQuery(PaymentConfirmationRequestEntity entity, PaymentConfirmationRequestListQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().toLowerCase(Locale.ROOT);
            boolean matched = containsIgnoreCase(entity.getInvoice().getInvoiceNumber(), keyword)
                    || containsIgnoreCase(entity.getCustomer().getCustomerCode(), keyword)
                    || containsIgnoreCase(entity.getCustomer().getCompanyName(), keyword)
                    || containsIgnoreCase(entity.getReferenceCode(), keyword)
                    || containsIgnoreCase(entity.getSenderAccountName(), keyword)
                    || containsIgnoreCase(entity.getSenderAccountNo(), keyword);
            if (!matched) {
                return false;
            }
        }
        if (StringUtils.hasText(query.getInvoiceId()) && !query.getInvoiceId().equalsIgnoreCase(entity.getInvoice().getId())) {
            return false;
        }
        if (StringUtils.hasText(query.getCustomerId()) && !query.getCustomerId().equalsIgnoreCase(entity.getCustomer().getId())) {
            return false;
        }
        if (StringUtils.hasText(query.getStatus()) && !query.getStatus().equalsIgnoreCase(entity.getStatus())) {
            return false;
        }
        if (query.getCreatedFrom() != null && (entity.getCreatedAt() == null || entity.getCreatedAt().toLocalDate().isBefore(query.getCreatedFrom()))) {
            return false;
        }
        return query.getCreatedTo() == null
                || (entity.getCreatedAt() != null && !entity.getCreatedAt().toLocalDate().isAfter(query.getCreatedTo()));
    }

    private Comparator<PaymentConfirmationRequestEntity> buildComparator(PaymentConfirmationRequestListQuery query) {
        Comparator<PaymentConfirmationRequestEntity> comparator = switch (query.getSortBy()) {
            case "invoiceNumber" -> Comparator.comparing(entity -> normalizeNullable(entity.getInvoice().getInvoiceNumber()), Comparator.nullsLast(String::compareToIgnoreCase));
            case "customerName" -> Comparator.comparing(entity -> normalizeNullable(entity.getCustomer().getCompanyName()), Comparator.nullsLast(String::compareToIgnoreCase));
            case "transferTime" -> Comparator.comparing(PaymentConfirmationRequestEntity::getTransferTime, Comparator.nullsLast(LocalDateTime::compareTo));
            case "requestedAmount" -> Comparator.comparing(entity -> normalizeMoney(entity.getRequestedAmount()), Comparator.nullsLast(BigDecimal::compareTo));
            case "status" -> Comparator.comparing(entity -> normalizeUpper(entity.getStatus()), String::compareToIgnoreCase);
            case "reviewedAt" -> Comparator.comparing(PaymentConfirmationRequestEntity::getReviewedAt, Comparator.nullsLast(LocalDateTime::compareTo));
            default -> Comparator.comparing(PaymentConfirmationRequestEntity::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        };
        return "desc".equalsIgnoreCase(query.getSortDir()) ? comparator.reversed() : comparator;
    }

    private void validateInvoiceCanReceiveConfirmation(InvoiceResponse invoice, BigDecimal amount, String fieldName) {
        if (!PAYABLE_INVOICE_STATUSES.contains(normalizeUpper(invoice.status()))) {
            throw RequestValidationException.singleError("invoiceId", "Invoice is not eligible for payment confirmation");
        }
        if (invoice.outstandingAmount().compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError("invoiceId", "Invoice has no outstanding balance");
        }
        if (amount != null && normalizeMoney(amount).compareTo(invoice.outstandingAmount()) > 0) {
            throw RequestValidationException.singleError(fieldName, fieldName + " must not exceed invoice outstanding amount");
        }
    }

    private void validatePendingReview(PaymentConfirmationRequestEntity entity) {
        if (!"PENDING_REVIEW".equalsIgnoreCase(entity.getStatus())) {
            throw RequestValidationException.singleError("requestId", "Only pending payment confirmation requests can be reviewed");
        }
    }

    private void normalizeAndValidateCreateRequest(PaymentConfirmationRequestCreateRequest request) {
        request.setRequestedAmount(normalizePositiveMoney(request.getRequestedAmount(), "requestedAmount", "requestedAmount must be greater than 0"));
        if (request.getTransferTime() == null) {
            throw RequestValidationException.singleError("transferTime", "transferTime is required");
        }
        request.setSenderBankName(normalizeRequired(request.getSenderBankName(), "senderBankName", "senderBankName is required"));
        request.setSenderAccountName(normalizeRequired(request.getSenderAccountName(), "senderAccountName", "senderAccountName is required"));
        request.setSenderAccountNo(normalizeRequired(request.getSenderAccountNo(), "senderAccountNo", "senderAccountNo is required"));
        request.setReferenceCode(normalizeRequired(request.getReferenceCode(), "referenceCode", "referenceCode is required"));
        request.setProofDocumentUrl(normalizeNullable(request.getProofDocumentUrl()));
        request.setNote(normalizeNullable(request.getNote()));
    }

    private void normalizeAndValidateConfirmRequest(PaymentConfirmationRequestConfirmRequest request) {
        request.setConfirmedAmount(normalizePositiveMoney(request.getConfirmedAmount(), "confirmedAmount", "confirmedAmount must be greater than 0"));
        if (request.getReviewNote() != null) {
            request.setReviewNote(normalizeNullable(request.getReviewNote()));
        }
    }

    private void normalizeAndValidateListQuery(PaymentConfirmationRequestListQuery query) {
        query.setKeyword(normalizeNullable(query.getKeyword()));
        query.setInvoiceId(normalizeNullable(query.getInvoiceId()));
        query.setCustomerId(normalizeNullable(query.getCustomerId()));
        query.setStatus(StringUtils.hasText(query.getStatus()) ? normalizeUpper(query.getStatus()) : null);
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "createdAt");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");
        if (query.getCreatedFrom() != null && query.getCreatedTo() != null && query.getCreatedFrom().isAfter(query.getCreatedTo())) {
            throw RequestValidationException.singleError("createdFrom", "createdFrom must be on or before createdTo");
        }
        if (StringUtils.hasText(query.getStatus()) && !ALLOWED_REQUEST_STATUSES.contains(query.getStatus())) {
            throw RequestValidationException.singleError("status", "status must be one of PENDING_REVIEW, CONFIRMED, REJECTED, CANCELLED");
        }
        if (!ALLOWED_SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of createdAt, transferTime, requestedAmount, status, reviewedAt, invoiceNumber, customerName");
        }
        if (!ALLOWED_SORT_DIRECTIONS.contains(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private AuthenticatedUser requireCustomer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (roleOf(currentUser) != RoleName.CUSTOMER) {
            throw new ForbiddenOperationException("Only customer users can create payment confirmation requests");
        }
        return currentUser;
    }

    private AuthenticatedUser requireReviewer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        RoleName role = roleOf(currentUser);
        if (role != RoleName.ACCOUNTANT && role != RoleName.OWNER) {
            throw new ForbiddenOperationException("Only accountant or owner users can review payment confirmation requests");
        }
        return currentUser;
    }

    private CustomerProfileEntity loadCurrentCustomer() {
        return customerProfileRepository.findByUser_Id(currentUserProvider.getCurrentUser().userId())
                .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private RoleName roleOf(AuthenticatedUser currentUser) {
        return RoleName.from(currentUser.role());
    }

    private void maybeSendReviewNotification(
            PaymentConfirmationRequestEntity entity,
            InvoiceResponse invoice,
            String templateName,
            String subject
    ) {
        if (entity.getCustomer() == null || !StringUtils.hasText(entity.getCustomer().getEmail())) {
            return;
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("recipientName", firstNonBlank(entity.getCustomer().getContactPerson(), entity.getCustomer().getCompanyName()));
        variables.put("customerName", entity.getCustomer().getCompanyName());
        variables.put("invoiceNumber", invoice.invoiceNumber());
        variables.put("requestedAmount", normalizeMoney(entity.getRequestedAmount()));
        variables.put("confirmedAmount", normalizeMoney(entity.getConfirmedAmount()));
        variables.put("outstandingAmount", invoice.outstandingAmount());
        variables.put("status", normalizeUpper(entity.getStatus()));
        variables.put("referenceCode", entity.getReferenceCode());
        variables.put("reviewNote", entity.getReviewNote());
        variables.put("proofDocumentUrl", entity.getProofDocumentUrl());
        variables.put("paymentId", entity.getPaymentId());
        emailService.sendHtmlEmail(entity.getCustomer().getEmail(), subject, templateName, variables).join();
    }

    private Map<String, Object> buildPaymentAuditPayload(PaymentEntity payment, PaymentConfirmationRequestEntity request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("customerId", payment.getCustomerId());
        payload.put("amount", normalizeMoney(payment.getAmount()));
        payload.put("paymentDate", payment.getPaymentDate());
        payload.put("paymentMethod", payment.getPaymentMethod());
        payload.put("referenceNo", payment.getReferenceNo());
        payload.put("proofDocumentUrl", payment.getProofDocumentUrl());
        payload.put("invoiceId", request.getInvoice().getId());
        payload.put("invoiceNumber", request.getInvoice().getInvoiceNumber());
        payload.put("requestId", request.getId());
        return payload;
    }

    private void logAudit(String action, String entityType, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
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

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositiveMoney(BigDecimal value, String field, String message) {
        BigDecimal normalized = value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
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

    private boolean containsIgnoreCase(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
