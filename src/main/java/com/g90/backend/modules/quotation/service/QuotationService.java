package com.g90.backend.modules.quotation.service;

import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;

public interface QuotationService {

    QuotationPreviewResponseData previewQuotation(QuotationSubmitRequest request);

    QuotationResponseData createQuotation(QuotationSubmitRequest request);

    QuotationResponseData saveDraftQuotation(QuotationSubmitRequest request);
}
