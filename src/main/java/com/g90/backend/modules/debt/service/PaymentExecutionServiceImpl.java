package com.g90.backend.modules.debt.service;

import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.debt.entity.DebtInvoiceEntity;
import com.g90.backend.modules.debt.entity.PaymentAllocationEntity;
import com.g90.backend.modules.debt.entity.PaymentEntity;
import com.g90.backend.modules.debt.repository.DebtInvoiceRepository;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.debt.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaymentExecutionServiceImpl implements PaymentExecutionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("CASH", "BANK_TRANSFER", "OTHER");
    private static final Set<String> SKIPPED_INVOICE_STATUSES = Set.of("DRAFT", "CANCELLED", "VOID");

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final DebtInvoiceRepository debtInvoiceRepository;

    public PaymentExecutionServiceImpl(
            PaymentRepository paymentRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            DebtInvoiceRepository debtInvoiceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.debtInvoiceRepository = debtInvoiceRepository;
    }

    @Override
    @Transactional
    public PaymentEntity recordPayment(PaymentExecutionCommand command) {
        String customerId = normalizeRequired(command.customerId(), "customerId", "customerId is required");
        if (command.paymentDate() == null) {
            throw RequestValidationException.singleError("paymentDate", "paymentDate is required");
        }
        BigDecimal amount = normalizePositiveMoney(command.amount(), "amount", "amount must be greater than 0");
        String paymentMethod = resolvePaymentMethod(command.paymentMethod());
        String referenceNo = normalizeNullable(command.referenceNo());
        String note = normalizeNullable(command.note());
        String proofDocumentUrl = normalizeNullable(command.proofDocumentUrl());
        String status = resolveStatus(command.status());
        String actorUserId = normalizeRequired(command.actorUserId(), "actorUserId", "actorUserId is required");
        List<PaymentExecutionCommand.PaymentExecutionAllocation> allocations = normalizeAllocations(command.allocations(), amount);

        if ("BANK_TRANSFER".equals(paymentMethod) && !StringUtils.hasText(referenceNo)) {
            throw RequestValidationException.singleError("referenceNo", "referenceNo is required for bank transfer payments");
        }
        if (paymentRepository.existsDuplicate(customerId, command.paymentDate(), amount, paymentMethod, referenceNo)) {
            throw RequestValidationException.singleError("referenceNo", "Duplicate payment detected");
        }

        Map<String, DebtInvoiceEntity> invoices = loadInvoices(allocations, customerId);
        Map<String, BigDecimal> outstandingByInvoiceId = buildOutstandingMap(invoices.values(), true);
        for (PaymentExecutionCommand.PaymentExecutionAllocation allocation : allocations) {
            BigDecimal outstandingAmount = outstandingByInvoiceId.get(allocation.invoiceId());
            if (outstandingAmount == null) {
                throw RequestValidationException.singleError("allocations", "Invoice " + allocation.invoiceId() + " is not available for allocation");
            }
            if (allocation.amount().compareTo(outstandingAmount) > 0) {
                throw RequestValidationException.singleError("allocations", "Allocation amount exceeds invoice outstanding balance");
            }
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setCustomerId(customerId);
        payment.setAmount(amount);
        payment.setPaymentDate(command.paymentDate());
        payment.setPaymentMethod(paymentMethod);
        payment.setReferenceNo(referenceNo);
        payment.setNote(note);
        payment.setStatus(status);
        payment.setProofDocumentUrl(proofDocumentUrl);
        payment.setCreatedBy(actorUserId);
        payment.setUpdatedBy(actorUserId);

        for (PaymentExecutionCommand.PaymentExecutionAllocation allocation : allocations) {
            PaymentAllocationEntity entity = new PaymentAllocationEntity();
            entity.setPayment(payment);
            entity.setInvoice(invoices.get(allocation.invoiceId()));
            entity.setAmount(allocation.amount());
            payment.getAllocations().add(entity);
        }

        PaymentEntity savedPayment = paymentRepository.save(payment);
        updateTouchedInvoiceStatuses(new ArrayList<>(invoices.values()));
        return savedPayment;
    }

    private Map<String, DebtInvoiceEntity> loadInvoices(
            List<PaymentExecutionCommand.PaymentExecutionAllocation> allocations,
            String customerId
    ) {
        LinkedHashSet<String> invoiceIds = new LinkedHashSet<>();
        for (PaymentExecutionCommand.PaymentExecutionAllocation allocation : allocations) {
            invoiceIds.add(allocation.invoiceId());
        }
        Map<String, DebtInvoiceEntity> invoices = new LinkedHashMap<>();
        for (DebtInvoiceEntity invoice : debtInvoiceRepository.findByIdIn(invoiceIds)) {
            if (!customerId.equalsIgnoreCase(invoice.getCustomerId())) {
                throw RequestValidationException.singleError("allocations", "Allocated invoice does not belong to the selected customer");
            }
            String baseStatus = normalizeUpper(invoice.getStatus());
            if (SKIPPED_INVOICE_STATUSES.contains(baseStatus)) {
                throw RequestValidationException.singleError("allocations", "Allocated invoice is not open for payment");
            }
            invoices.put(invoice.getId(), invoice);
        }
        if (invoices.size() != invoiceIds.size()) {
            throw RequestValidationException.singleError("allocations", "One or more allocated invoices were not found");
        }
        return invoices;
    }

    private Map<String, BigDecimal> buildOutstandingMap(Collection<DebtInvoiceEntity> invoices, boolean requireOpenBalance) {
        Map<String, BigDecimal> outstandingMap = new LinkedHashMap<>();
        if (invoices.isEmpty()) {
            return outstandingMap;
        }
        List<String> invoiceIds = invoices.stream().map(DebtInvoiceEntity::getId).toList();
        Map<String, BigDecimal> allocatedMap = new LinkedHashMap<>();
        for (PaymentAllocationRepository.InvoiceAllocationTotalView view : paymentAllocationRepository.summarizeByInvoiceIds(invoiceIds)) {
            allocatedMap.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount()));
        }
        for (DebtInvoiceEntity invoice : invoices) {
            BigDecimal grandTotal = normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paidAmount = allocatedMap.getOrDefault(invoice.getId(), ZERO).min(grandTotal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingAmount = grandTotal.subtract(paidAmount).max(ZERO).setScale(2, RoundingMode.HALF_UP);
            if (requireOpenBalance && remainingAmount.compareTo(ZERO) <= 0) {
                throw RequestValidationException.singleError("allocations", "Allocated invoice is already fully paid");
            }
            outstandingMap.put(invoice.getId(), remainingAmount);
        }
        return outstandingMap;
    }

    private void updateTouchedInvoiceStatuses(List<DebtInvoiceEntity> invoices) {
        if (invoices.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> outstandingByInvoiceId = buildOutstandingMap(invoices, false);
        List<DebtInvoiceEntity> touchedInvoices = new ArrayList<>();
        for (DebtInvoiceEntity invoice : invoices) {
            BigDecimal remainingAmount = outstandingByInvoiceId.get(invoice.getId());
            if (remainingAmount == null) {
                continue;
            }
            invoice.setStatus(remainingAmount.compareTo(ZERO) <= 0 ? "PAID" : "PARTIALLY_PAID");
            touchedInvoices.add(invoice);
        }
        if (!touchedInvoices.isEmpty()) {
            debtInvoiceRepository.saveAll(touchedInvoices);
        }
    }

    private List<PaymentExecutionCommand.PaymentExecutionAllocation> normalizeAllocations(
            List<PaymentExecutionCommand.PaymentExecutionAllocation> allocations,
            BigDecimal paymentAmount
    ) {
        if (allocations == null || allocations.isEmpty()) {
            throw RequestValidationException.singleError("allocations", "allocations are required");
        }
        Map<String, BigDecimal> uniqueAllocations = new LinkedHashMap<>();
        BigDecimal totalAmount = ZERO;
        for (PaymentExecutionCommand.PaymentExecutionAllocation allocation : allocations) {
            String invoiceId = normalizeRequired(allocation.invoiceId(), "allocations.invoiceId", "invoiceId is required");
            BigDecimal allocationAmount = normalizePositiveMoney(allocation.amount(), "allocations.amount", "allocation amount must be greater than 0");
            if (uniqueAllocations.put(invoiceId, allocationAmount) != null) {
                throw RequestValidationException.singleError("allocations", "Duplicate invoice allocation is not allowed");
            }
            totalAmount = totalAmount.add(allocationAmount).setScale(2, RoundingMode.HALF_UP);
        }
        if (totalAmount.compareTo(paymentAmount) != 0) {
            throw RequestValidationException.singleError("allocations", "Allocation total must equal payment amount");
        }
        return uniqueAllocations.entrySet().stream()
                .map(entry -> new PaymentExecutionCommand.PaymentExecutionAllocation(entry.getKey(), entry.getValue()))
                .toList();
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

    private String resolveStatus(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "CONFIRMED";
        if (!"CONFIRMED".equals(normalized)) {
            throw RequestValidationException.singleError("status", "Only CONFIRMED payment status is supported");
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
}
