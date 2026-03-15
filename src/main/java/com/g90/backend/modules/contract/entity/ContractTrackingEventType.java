package com.g90.backend.modules.contract.entity;

public enum ContractTrackingEventType {
    DRAFT_CREATED,
    UPDATED,
    APPROVAL_REQUESTED,
    APPROVED,
    REJECTED,
    SUBMITTED,
    INVENTORY_RESERVED,
    CANCEL_REQUESTED,
    CANCELLED,
    PICKED,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    DOCUMENT_GENERATED,
    DOCUMENT_EXPORTED,
    DOCUMENT_EMAILED
}
