package com.g90.backend.modules.account.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record AccountListResponseData(
        List<AccountListItemResponse> content,
        int page,
        int size,
        long totalElements
) {
}
