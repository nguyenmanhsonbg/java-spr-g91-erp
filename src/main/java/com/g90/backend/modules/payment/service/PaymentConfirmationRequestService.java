package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestConfirmRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestCreateRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListQuery;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestListResponseData;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestRejectRequest;
import com.g90.backend.modules.payment.dto.PaymentConfirmationRequestResponse;

public interface PaymentConfirmationRequestService {

    PaymentConfirmationRequestResponse createRequest(String invoiceId, PaymentConfirmationRequestCreateRequest request);

    PaymentConfirmationRequestListResponseData listRequests(PaymentConfirmationRequestListQuery query);

    PaymentConfirmationRequestResponse getDetail(String requestId);

    PaymentConfirmationRequestResponse confirmRequest(String requestId, PaymentConfirmationRequestConfirmRequest request);

    PaymentConfirmationRequestResponse rejectRequest(String requestId, PaymentConfirmationRequestRejectRequest request);

    PaymentConfirmationRequestListResponseData listRequestsByInvoice(String invoiceId, PaymentConfirmationRequestListQuery query);
}
