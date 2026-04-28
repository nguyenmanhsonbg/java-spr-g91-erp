package com.g90.backend.modules.payment.service;

import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.contract.entity.ContractEntity;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.debt.repository.PaymentAllocationRepository;
import com.g90.backend.modules.payment.entity.InvoiceEntity;
import com.g90.backend.modules.payment.entity.InvoiceItemEntity;
import com.g90.backend.modules.payment.repository.InvoiceRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContractBillingServiceImpl implements ContractBillingService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal THIRTY_PERCENT = new BigDecimal("30.00");
    private static final BigDecimal VAT_WITH_TAX_CODE = new BigDecimal("10.00");
    private static final String PAYMENT_OPTION_FULL = "TT01";
    private static final String PAYMENT_OPTION_DEPOSIT = "TT02";
    private static final Set<String> ACTIVE_EXCLUDED_STATUSES = Set.of("CANCELLED", "VOID");
    private static final Set<String> FULFILLMENT_STATUSES = Set.of(
            ContractStatus.SUBMITTED.name(),
            ContractStatus.PROCESSING.name(),
            ContractStatus.RESERVED.name(),
            ContractStatus.PICKED.name(),
            ContractStatus.IN_TRANSIT.name(),
            ContractStatus.DELIVERED.name(),
            ContractStatus.COMPLETED.name()
    );
    private static final Set<String> FINAL_INVOICE_STATUSES = Set.of(
            ContractStatus.DELIVERED.name(),
            ContractStatus.COMPLETED.name()
    );

    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    public ContractBillingServiceImpl(
            InvoiceRepository invoiceRepository,
            PaymentAllocationRepository paymentAllocationRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
    }

    @Override
    @Transactional
    public void ensureInitialBillingInvoice(ContractEntity contract, String actingUserId) {
        if (contract == null || !FULFILLMENT_STATUSES.contains(normalizeUpper(contract.getStatus()))) {
            return;
        }
        if (isFullUpfront(contract)) {
            createPhaseInvoiceIfMissing(
                    contract,
                    PHASE_FULL,
                    contractTotal(contract),
                    "Full upfront payment for contract " + contractNumber(contract),
                    "100% upfront payment",
                    actingUserId,
                    LocalDate.now(APP_ZONE)
            );
            return;
        }
        if (isDepositOption(contract)) {
            createPhaseInvoiceIfMissing(
                    contract,
                    PHASE_DEPOSIT,
                    depositAmount(contract),
                    "Deposit payment for contract " + contractNumber(contract),
                    "Contract deposit payment",
                    actingUserId,
                    LocalDate.now(APP_ZONE)
            );
        }
    }

    @Override
    public String resolveRequestedBillingPhase(ContractEntity contract, String requestedPhase) {
        String normalized = normalizeUpper(requestedPhase);
        if (StringUtils.hasText(normalized)) {
            validateKnownPhase(normalized);
            return normalized;
        }
        if (isDepositOption(contract) && FINAL_INVOICE_STATUSES.contains(normalizeUpper(contract.getStatus()))) {
            return PHASE_FINAL;
        }
        if (isFullUpfront(contract)) {
            return PHASE_FULL;
        }
        return PHASE_GENERAL;
    }

    @Override
    public void validateInvoiceCreation(ContractEntity contract, String billingPhase, BigDecimal invoiceTotalAmount, String currentInvoiceId) {
        String phase = normalizePhase(billingPhase);
        validateKnownPhase(phase);
        BigDecimal totalAmount = normalizeMoney(invoiceTotalAmount);
        if (totalAmount.compareTo(ZERO) <= 0) {
            throw RequestValidationException.singleError("totalAmount", "Invoice total amount must be greater than 0");
        }

        if (PHASE_FULL.equals(phase)) {
            ensurePaymentOption(contract, PAYMENT_OPTION_FULL, "billingPhase FULL is only allowed for 100% upfront contracts");
            validateExactAmount(totalAmount, contractTotal(contract), "billingPhase", "FULL invoice amount must equal contract total amount");
            ensureSingleActivePhase(contract, phase, currentInvoiceId);
            return;
        }
        if (PHASE_DEPOSIT.equals(phase)) {
            ensurePaymentOption(contract, PAYMENT_OPTION_DEPOSIT, "billingPhase DEPOSIT is only allowed for deposit contracts");
            validateExactAmount(totalAmount, depositAmount(contract), "billingPhase", "DEPOSIT invoice amount must equal contract deposit amount");
            ensureSingleActivePhase(contract, phase, currentInvoiceId);
            return;
        }
        if (PHASE_FINAL.equals(phase)) {
            ensurePaymentOption(contract, PAYMENT_OPTION_DEPOSIT, "billingPhase FINAL is only allowed for deposit contracts");
            if (!FINAL_INVOICE_STATUSES.contains(normalizeUpper(contract.getStatus()))) {
                throw RequestValidationException.singleError("billingPhase", "FINAL invoice can only be created after sale order delivery");
            }
            BigDecimal finalLimit = finalAmount(contract);
            BigDecimal existing = activePhaseTotal(contract, PHASE_FINAL, currentInvoiceId);
            if (existing.add(totalAmount).setScale(2, RoundingMode.HALF_UP).compareTo(finalLimit) > 0) {
                throw RequestValidationException.singleError("billingPhase", "FINAL invoice amount exceeds remaining contract balance");
            }
            return;
        }

        ensureSingleActiveGeneralInvoice(contract, currentInvoiceId);
    }

    @Override
    public BigDecimal remainingPhaseAmount(ContractEntity contract, String billingPhase, String currentInvoiceId) {
        String phase = normalizePhase(billingPhase);
        if (PHASE_DEPOSIT.equals(phase)) {
            return depositAmount(contract).subtract(activePhaseTotal(contract, PHASE_DEPOSIT, currentInvoiceId)).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        if (PHASE_FULL.equals(phase)) {
            return contractTotal(contract).subtract(activePhaseTotal(contract, PHASE_FULL, currentInvoiceId)).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        if (PHASE_FINAL.equals(phase)) {
            return finalAmount(contract).subtract(activePhaseTotal(contract, PHASE_FINAL, currentInvoiceId)).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        return contractTotal(contract);
    }

    @Override
    public PaymentGateStatus evaluatePaymentGate(ContractEntity contract) {
        if (contract == null) {
            return openGate(false);
        }
        if (isFullUpfront(contract)) {
            BigDecimal required = contractTotal(contract);
            PhaseSnapshot snapshot = phaseSnapshot(contract, PHASE_FULL);
            boolean paid = phaseSatisfied(contract, snapshot, required);
            return new PaymentGateStatus(
                    true,
                    paid,
                    paid,
                    paid ? null : "Full upfront payment must be confirmed before warehouse fulfillment",
                    grandTotal(contract, required),
                    snapshot.paidAmount(),
                    ZERO,
                    ZERO
            );
        }
        if (isDepositOption(contract)) {
            BigDecimal upfrontRequired = depositAmount(contract);
            BigDecimal finalRequired = finalAmount(contract);
            PhaseSnapshot deposit = phaseSnapshot(contract, PHASE_DEPOSIT);
            PhaseSnapshot finalPayment = phaseSnapshot(contract, PHASE_FINAL);
            boolean depositPaid = phaseSatisfied(contract, deposit, upfrontRequired);
            boolean finalPaid = phaseSatisfied(contract, finalPayment, finalRequired);
            String blockedReason = null;
            if (!depositPaid) {
                blockedReason = "Deposit payment must be confirmed before warehouse fulfillment";
            } else if (!finalPaid) {
                blockedReason = "Final payment must be fully confirmed before completing the sale order";
            }
            return new PaymentGateStatus(
                    true,
                    depositPaid,
                    depositPaid && finalPaid,
                    blockedReason,
                    grandTotal(contract, upfrontRequired),
                    deposit.paidAmount(),
                    grandTotal(contract, finalRequired),
                    finalPayment.paidAmount()
            );
        }
        return openGate(false);
    }

    @Override
    public boolean isDepositPaymentSatisfied(ContractEntity contract) {
        if (!isDepositOption(contract)) {
            return false;
        }
        return phaseSatisfied(contract, phaseSnapshot(contract, PHASE_DEPOSIT), depositAmount(contract));
    }

    @Override
    public BigDecimal paidDepositAmount(ContractEntity contract) {
        if (!isDepositOption(contract)) {
            return ZERO;
        }
        return phaseSnapshot(contract, PHASE_DEPOSIT).paidAmount();
    }

    private void createPhaseInvoiceIfMissing(
            ContractEntity contract,
            String phase,
            BigDecimal amount,
            String description,
            String paymentTerms,
            String actingUserId,
            LocalDate dueDate
    ) {
        if (hasActivePhaseInvoice(contract, phase, null)) {
            return;
        }
        createBillingInvoice(contract, phase, amount, description, paymentTerms, actingUserId, dueDate);
    }

    private InvoiceEntity createBillingInvoice(
            ContractEntity contract,
            String phase,
            BigDecimal amount,
            String description,
            String paymentTerms,
            String actingUserId,
            LocalDate dueDate
    ) {
        BigDecimal normalizedAmount = normalizeMoney(amount);
        if (normalizedAmount.compareTo(ZERO) <= 0) {
            return null;
        }
        CustomerProfileEntity customer = contract.getCustomer();
        BigDecimal vatRate = hasTaxCode(customer) ? VAT_WITH_TAX_CODE : ZERO;
        BigDecimal vatAmount = normalizedAmount.multiply(vatRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        LocalDate issueDate = LocalDate.now(APP_ZONE);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber(generateInvoiceNumber(issueDate));
        invoice.setContract(contract);
        invoice.setCustomer(customer);
        invoice.setSourceType("CONTRACT_BILLING");
        invoice.setBillingPhase(phase);
        invoice.setCustomerName(customer == null ? null : normalizeNullable(customer.getCompanyName()));
        invoice.setCustomerTaxCode(customer == null ? null : normalizeNullable(customer.getTaxCode()));
        invoice.setBillingAddress(customer == null ? null : normalizeNullable(customer.getAddress()));
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate == null ? issueDate : dueDate);
        invoice.setPaymentTerms(paymentTerms);
        invoice.setNote(description);
        invoice.setAdjustmentAmount(ZERO);
        invoice.setTotalAmount(normalizedAmount);
        invoice.setVatRate(vatRate);
        invoice.setVatAmount(vatAmount);
        invoice.setStatus("ISSUED");
        invoice.setCreatedBy(actingUserId);
        invoice.setUpdatedBy(actingUserId);
        invoice.setIssuedBy(actingUserId);
        invoice.setIssuedAt(LocalDateTime.now(APP_ZONE));

        InvoiceItemEntity item = new InvoiceItemEntity();
        item.setInvoice(invoice);
        item.setDescription(description);
        item.setUnit("CONTRACT");
        item.setQuantity(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP));
        item.setUnitPrice(normalizedAmount);
        item.setTotalPrice(normalizedAmount);
        invoice.getItems().add(item);
        return invoiceRepository.save(invoice);
    }

    private PaymentGateStatus openGate(boolean requiresGate) {
        return new PaymentGateStatus(requiresGate, true, true, null, ZERO, ZERO, ZERO, ZERO);
    }

    private boolean phaseSatisfied(ContractEntity contract, PhaseSnapshot snapshot, BigDecimal expectedAmount) {
        BigDecimal expected = normalizeMoney(expectedAmount);
        if (expected.compareTo(ZERO) <= 0) {
            return true;
        }
        if (snapshot.billedAmount().compareTo(expected) < 0 || snapshot.grandTotal().compareTo(ZERO) <= 0) {
            return false;
        }
        return snapshot.paidAmount().compareTo(snapshot.grandTotal()) >= 0;
    }

    private PhaseSnapshot phaseSnapshot(ContractEntity contract, String phase) {
        List<InvoiceEntity> invoices = activeInvoices(contract);
        List<String> ids = invoices.stream().map(InvoiceEntity::getId).toList();
        Map<String, BigDecimal> paidByInvoice = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (PaymentAllocationRepository.InvoiceAllocationTotalView view : paymentAllocationRepository.summarizeByInvoiceIds(ids)) {
                paidByInvoice.put(view.getInvoiceId(), normalizeMoney(view.getAllocatedAmount()));
            }
        }

        BigDecimal billedAmount = ZERO;
        BigDecimal grandTotal = ZERO;
        BigDecimal paidAmount = ZERO;
        for (InvoiceEntity invoice : invoices) {
            if (!phase.equals(normalizePhase(invoice.getBillingPhase()))) {
                continue;
            }
            BigDecimal invoiceGrandTotal = grandTotal(invoice);
            billedAmount = billedAmount.add(normalizeMoney(invoice.getTotalAmount())).setScale(2, RoundingMode.HALF_UP);
            grandTotal = grandTotal.add(invoiceGrandTotal).setScale(2, RoundingMode.HALF_UP);
            paidAmount = paidAmount.add(paidByInvoice.getOrDefault(invoice.getId(), ZERO).min(invoiceGrandTotal)).setScale(2, RoundingMode.HALF_UP);
        }
        return new PhaseSnapshot(billedAmount, grandTotal, paidAmount);
    }

    private BigDecimal activePhaseTotal(ContractEntity contract, String phase, String currentInvoiceId) {
        return activeInvoices(contract).stream()
                .filter(invoice -> currentInvoiceId == null || !currentInvoiceId.equals(invoice.getId()))
                .filter(invoice -> phase.equals(normalizePhase(invoice.getBillingPhase())))
                .map(InvoiceEntity::getTotalAmount)
                .map(this::normalizeMoney)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasActivePhaseInvoice(ContractEntity contract, String phase, String currentInvoiceId) {
        return activeInvoices(contract).stream()
                .filter(invoice -> currentInvoiceId == null || !currentInvoiceId.equals(invoice.getId()))
                .anyMatch(invoice -> phase.equals(normalizePhase(invoice.getBillingPhase())));
    }

    private void ensureSingleActivePhase(ContractEntity contract, String phase, String currentInvoiceId) {
        if (hasActivePhaseInvoice(contract, phase, currentInvoiceId)) {
            throw RequestValidationException.singleError("billingPhase", phase + " invoice already exists for this contract");
        }
    }

    private void ensureSingleActiveGeneralInvoice(ContractEntity contract, String currentInvoiceId) {
        boolean exists = activeInvoices(contract).stream()
                .filter(invoice -> currentInvoiceId == null || !currentInvoiceId.equals(invoice.getId()))
                .anyMatch(invoice -> PHASE_GENERAL.equals(normalizePhase(invoice.getBillingPhase())));
        if (exists) {
            throw RequestValidationException.singleError("contractId", "Contract already has an active general invoice");
        }
    }

    private List<InvoiceEntity> activeInvoices(ContractEntity contract) {
        if (contract == null || !StringUtils.hasText(contract.getId())) {
            return List.of();
        }
        return invoiceRepository.findByContractIdWithCustomerAndContract(contract.getId()).stream()
                .filter(invoice -> !ACTIVE_EXCLUDED_STATUSES.contains(normalizeUpper(invoice.getStatus())))
                .toList();
    }

    private void ensurePaymentOption(ContractEntity contract, String expectedCode, String message) {
        if (!expectedCode.equalsIgnoreCase(paymentOptionCode(contract))) {
            throw RequestValidationException.singleError("billingPhase", message);
        }
    }

    private void validateExactAmount(BigDecimal actual, BigDecimal expected, String field, String message) {
        if (normalizeMoney(actual).compareTo(normalizeMoney(expected)) != 0) {
            throw RequestValidationException.singleError(field, message);
        }
    }

    private void validateKnownPhase(String phase) {
        if (!Set.of(PHASE_GENERAL, PHASE_FULL, PHASE_DEPOSIT, PHASE_FINAL).contains(phase)) {
            throw RequestValidationException.singleError("billingPhase", "billingPhase must be one of GENERAL, FULL, DEPOSIT, FINAL");
        }
    }

    private BigDecimal contractTotal(ContractEntity contract) {
        return normalizeMoney(contract == null ? null : contract.getTotalAmount());
    }

    private BigDecimal depositAmount(ContractEntity contract) {
        BigDecimal storedDeposit = normalizeMoney(contract == null ? null : contract.getDepositAmount());
        if (storedDeposit.compareTo(ZERO) > 0) {
            return storedDeposit;
        }
        return contractTotal(contract).multiply(depositPercentage(contract)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal finalAmount(ContractEntity contract) {
        return contractTotal(contract).subtract(depositAmount(contract)).max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal depositPercentage(ContractEntity contract) {
        BigDecimal value = normalizeMoney(contract == null ? null : contract.getDepositPercentage());
        return value.compareTo(ZERO) > 0 ? value : THIRTY_PERCENT;
    }

    private BigDecimal grandTotal(ContractEntity contract, BigDecimal totalAmount) {
        BigDecimal total = normalizeMoney(totalAmount);
        BigDecimal vatRate = hasTaxCode(contract == null ? null : contract.getCustomer()) ? VAT_WITH_TAX_CODE : ZERO;
        return total.add(total.multiply(vatRate).divide(HUNDRED, 2, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal grandTotal(InvoiceEntity invoice) {
        return normalizeMoney(invoice.getTotalAmount()).add(normalizeMoney(invoice.getVatAmount())).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateInvoiceNumber(LocalDate issueDate) {
        LocalDate effectiveDate = issueDate == null ? LocalDate.now(APP_ZONE) : issueDate;
        long sequence = invoiceRepository.countByIssueDateBetween(
                LocalDate.of(effectiveDate.getYear(), 1, 1),
                LocalDate.of(effectiveDate.getYear(), 12, 31)
        ) + 1;
        return "INV-" + effectiveDate.getYear() + "-" + String.format(Locale.ROOT, "%04d", sequence);
    }

    private boolean isFullUpfront(ContractEntity contract) {
        return PAYMENT_OPTION_FULL.equalsIgnoreCase(paymentOptionCode(contract));
    }

    private boolean isDepositOption(ContractEntity contract) {
        return PAYMENT_OPTION_DEPOSIT.equalsIgnoreCase(paymentOptionCode(contract));
    }

    private String paymentOptionCode(ContractEntity contract) {
        return contract == null || contract.getPaymentOption() == null ? "" : normalizeUpper(contract.getPaymentOption().getCode());
    }

    private String contractNumber(ContractEntity contract) {
        return contract == null || !StringUtils.hasText(contract.getContractNumber()) ? "UNKNOWN" : contract.getContractNumber();
    }

    private boolean hasTaxCode(CustomerProfileEntity customer) {
        return customer != null && StringUtils.hasText(customer.getTaxCode());
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePhase(String value) {
        String normalized = normalizeUpper(value);
        return StringUtils.hasText(normalized) ? normalized : PHASE_GENERAL;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record PhaseSnapshot(
            BigDecimal billedAmount,
            BigDecimal grandTotal,
            BigDecimal paidAmount
    ) {
    }
}
