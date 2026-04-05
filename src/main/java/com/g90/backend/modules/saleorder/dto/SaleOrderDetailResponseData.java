package com.g90.backend.modules.saleorder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SaleOrderDetailResponseData(
        HeaderData header,
        CustomerData customer,
        ProjectData project,
        List<ItemData> items,
        FulfillmentSummaryData fulfillment,
        List<TimelineEventData> timeline,
        List<InventoryIssueData> inventoryIssues,
        List<InvoiceData> invoices
) {
    public record HeaderData(
            String id,
            String saleOrderNumber,
            String contractId,
            String contractNumber,
            String status,
            LocalDate orderDate,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            LocalDate expectedDeliveryDate,
            LocalDate actualDeliveryDate,
            BigDecimal totalAmount,
            String note
    ) {
    }

    public record CustomerData(
            String id,
            String customerCode,
            String companyName,
            String contactPerson,
            String phone,
            String email,
            String address
    ) {
    }

    public record ProjectData(
            String id,
            String projectCode,
            String projectName,
            String status
    ) {
    }

    public record ItemData(
            String id,
            String productId,
            String productCode,
            String productName,
            String type,
            String size,
            String thickness,
            String unit,
            BigDecimal orderedQuantity,
            BigDecimal reservedQuantity,
            BigDecimal issuedQuantity,
            BigDecimal deliveredQuantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount,
            String note
    ) {
    }

    public record FulfillmentSummaryData(
            BigDecimal orderedQuantity,
            BigDecimal reservedQuantity,
            BigDecimal issuedQuantity,
            BigDecimal deliveredQuantity,
            Integer totalItems,
            Integer inventoryIssueCount,
            Integer invoiceCount
    ) {
    }

    public record TimelineEventData(
            String eventType,
            String eventStatus,
            String title,
            String note,
            LocalDateTime expectedAt,
            LocalDateTime actualAt,
            String trackingNumber
    ) {
    }

    public record InventoryIssueData(
            String transactionId,
            String transactionCode,
            String productId,
            String productCode,
            String productName,
            BigDecimal quantity,
            LocalDateTime transactionDate,
            String reason,
            String note
    ) {
    }

    public record InvoiceData(
            String invoiceId,
            String invoiceNumber,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String status,
            String documentUrl
    ) {
    }
}
