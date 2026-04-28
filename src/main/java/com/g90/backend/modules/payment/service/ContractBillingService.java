package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.contract.entity.ContractEntity;
import java.math.BigDecimal;

public interface ContractBillingService {

    String PHASE_GENERAL = "GENERAL";
    String PHASE_FULL = "FULL";
    String PHASE_DEPOSIT = "DEPOSIT";
    String PHASE_FINAL = "FINAL";

    void ensureInitialBillingInvoice(ContractEntity contract, String actingUserId);

    String resolveRequestedBillingPhase(ContractEntity contract, String requestedPhase);

    void validateInvoiceCreation(ContractEntity contract, String billingPhase, BigDecimal invoiceTotalAmount, String currentInvoiceId);

    BigDecimal remainingPhaseAmount(ContractEntity contract, String billingPhase, String currentInvoiceId);

    PaymentGateStatus evaluatePaymentGate(ContractEntity contract);

    boolean isDepositPaymentSatisfied(ContractEntity contract);

    BigDecimal paidDepositAmount(ContractEntity contract);

    record PaymentGateStatus(
            boolean requiresGate,
            boolean canStartFulfillment,
            boolean canComplete,
            String blockedReason,
            BigDecimal upfrontRequiredAmount,
            BigDecimal upfrontPaidAmount,
            BigDecimal finalRequiredAmount,
            BigDecimal finalPaidAmount
    ) {
    }
}
