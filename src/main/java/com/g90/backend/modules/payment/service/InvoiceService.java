package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.InvoiceCancelRequest;
import com.g90.backend.modules.payment.dto.ConvertContractToInvoiceRequest;
import com.g90.backend.modules.payment.dto.InvoiceCreateRequest;
import com.g90.backend.modules.payment.dto.InvoiceListQuery;
import com.g90.backend.modules.payment.dto.InvoiceListResponseData;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.InvoiceUpdateRequest;

public interface InvoiceService {

    InvoiceResponse createInvoice(InvoiceCreateRequest request);

    InvoiceResponse convertContractToInvoice(String contractId, ConvertContractToInvoiceRequest request);

    InvoiceListResponseData getInvoices(InvoiceListQuery query);

    InvoiceResponse getInvoice(String invoiceId);

    InvoiceResponse updateInvoice(String invoiceId, InvoiceUpdateRequest request);

    InvoiceResponse cancelInvoice(String invoiceId, InvoiceCancelRequest request);
}
