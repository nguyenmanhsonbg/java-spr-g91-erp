package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.PaymentInstructionResponse;

public interface PaymentInstructionService {

    PaymentInstructionResponse getPaymentInstruction(String invoiceId);
}
