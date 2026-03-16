package com.g90.backend.modules.customer.dto;

public record CustomerCreateResponse(
        String id,
        String customerCode,
        String companyName,
        String status,
        PortalAccountData portalAccount
) {
    public record PortalAccountData(
            String userId,
            String email,
            String temporaryPassword
    ) {
    }
}
