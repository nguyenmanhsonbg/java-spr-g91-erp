package com.g90.backend.modules.debt.dto;

import java.util.List;

public record DebtSettlementListResponseData(
        List<SettlementResponse> items
) {
}
