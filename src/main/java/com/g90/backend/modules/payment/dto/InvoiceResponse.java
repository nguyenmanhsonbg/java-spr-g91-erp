package com.g90.backend.modules.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        String id,
        String invoiceNumber,
        String sourceType,
        String contractId,
        String contractNumber,
        String customerId,
        String customerCode,
        String customerName,
        String customerTaxCode,
        String billingAddress,
        String paymentTerms,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal subtotalAmount,
        BigDecimal adjustmentAmount,
        BigDecimal totalAmount,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal grandTotal,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        String status,
        String note,
        String documentUrl,
        String cancellationReason,
        String createdBy,
        String updatedBy,
        String issuedBy,
        String cancelledBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime issuedAt,
        LocalDateTime cancelledAt,
        LocalDateTime notificationSentAt,
        List<Item> items,
        List<PaymentHistoryItem> paymentHistory
) {

    public record Item(
            String id,
            String productId,
            String productCode,
            String productName,
            String description,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
    }

    public record PaymentHistoryItem(
            String paymentId,
            String receiptNumber,
            LocalDate paymentDate,
            BigDecimal allocatedAmount,
            BigDecimal paymentAmount,
            String paymentMethod,
            String referenceNo,
            String note,
            String createdBy,
            LocalDateTime createdAt
    ) {
    }
}
