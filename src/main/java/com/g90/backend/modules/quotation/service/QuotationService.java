package com.g90.backend.modules.quotation.service;

import com.g90.backend.modules.quotation.dto.CustomerQuotationListQuery;
import com.g90.backend.modules.quotation.dto.CustomerQuotationListResponseData;
import com.g90.backend.modules.quotation.dto.CustomerQuotationSummaryResponseData;
import com.g90.backend.modules.quotation.dto.QuotationDetailResponseData;
import com.g90.backend.modules.quotation.dto.QuotationFormInitQuery;
import com.g90.backend.modules.quotation.dto.QuotationFormInitResponseData;
import com.g90.backend.modules.quotation.dto.QuotationHistoryResponseData;
import com.g90.backend.modules.quotation.dto.QuotationManagementListQuery;
import com.g90.backend.modules.quotation.dto.QuotationManagementListResponseData;
import com.g90.backend.modules.quotation.dto.QuotationPreviewByIdResponseData;
import com.g90.backend.modules.quotation.dto.QuotationPreviewResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSaveResponseData;
import com.g90.backend.modules.quotation.dto.QuotationSubmitActionRequest;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;
import com.g90.backend.modules.quotation.dto.QuotationSubmitResponseData;

public interface QuotationService {

    QuotationFormInitResponseData getQuotationFormInit(QuotationFormInitQuery query);

    QuotationPreviewResponseData previewQuotation(QuotationSubmitRequest request);

    QuotationSubmitResponseData createQuotation(QuotationSubmitRequest request);

    QuotationSaveResponseData saveDraftQuotation(QuotationSubmitRequest request);

    QuotationSubmitResponseData submitQuotation(QuotationSubmitActionRequest request);

    QuotationPreviewByIdResponseData getQuotationPreview(String quotationId);

    QuotationSaveResponseData updateDraftQuotation(String quotationId, QuotationSubmitRequest request);

    QuotationSubmitResponseData submitQuotation(String quotationId);

    CustomerQuotationListResponseData getMyQuotations(CustomerQuotationListQuery query);

    CustomerQuotationSummaryResponseData getMyQuotationSummary();

    QuotationManagementListResponseData getQuotations(QuotationManagementListQuery query);

    QuotationDetailResponseData getQuotationDetail(String quotationId);

    QuotationHistoryResponseData getQuotationHistory(String quotationId);
}
