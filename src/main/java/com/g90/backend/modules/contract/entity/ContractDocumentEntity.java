package com.g90.backend.modules.contract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "contract_documents")
public class ContractDocumentEntity {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @Column(name = "document_type", length = 30, nullable = false)
    private String documentType;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "preview_only")
    private boolean previewOnly;

    @Column(name = "official_document")
    private boolean officialDocument;

    @Column(name = "watermark_text", length = 50)
    private String watermarkText;

    @Column(name = "generation_status", length = 30, nullable = false)
    private String generationStatus;

    @Column(name = "export_count")
    private Integer exportCount;

    @Column(name = "generated_by", length = 36, nullable = false)
    private String generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "last_exported_by", length = 36)
    private String lastExportedBy;

    @Column(name = "last_exported_at")
    private LocalDateTime lastExportedAt;

    @Column(name = "emailed_by", length = 36)
    private String emailedBy;

    @Column(name = "emailed_at")
    private LocalDateTime emailedAt;

    @PrePersist
    public void prePersist() {
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }
        if (!StringUtils.hasText(generationStatus)) {
            generationStatus = "GENERATED";
        }
        if (exportCount == null) {
            exportCount = 0;
        }
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now(APP_ZONE);
        }
    }
}
