package com.g90.backend.modules.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ProjectInvoiceEntity")
@Table(name = "invoices")
public class ProjectInvoiceEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "contract_id", length = 36)
    private String contractId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "vat_amount", precision = 18, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
