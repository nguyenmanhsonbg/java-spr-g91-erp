package com.g90.backend.modules.debt.service;

import com.g90.backend.modules.debt.dto.OpenInvoiceResponse;
import com.g90.backend.modules.debt.dto.PaymentCreateRequest;
import com.g90.backend.modules.debt.dto.PaymentResponse;
import java.util.List;

public interface PaymentService {

    PaymentResponse recordPayment(PaymentCreateRequest request);

    PaymentResponse getPayment(String paymentId);

    List<OpenInvoiceResponse> getOpenInvoices(String customerId);
}
