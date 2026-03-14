package com.g90.backend.modules.quotation.dto;

public record CustomerQuotationSummaryResponseData(
        long total,
        long draft,
        long pending,
        long converted,
        long rejected
) {
}
