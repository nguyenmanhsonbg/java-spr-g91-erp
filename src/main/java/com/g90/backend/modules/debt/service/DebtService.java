package com.g90.backend.modules.debt.service;

import com.g90.backend.modules.debt.dto.DebtAgingResponseData;
import com.g90.backend.modules.debt.dto.DebtHistoryResponseData;
import com.g90.backend.modules.debt.dto.DebtListQuery;
import com.g90.backend.modules.debt.dto.DebtListResponseData;
import com.g90.backend.modules.debt.dto.DebtSettlementListResponseData;
import com.g90.backend.modules.debt.dto.DebtStatusResponseData;
import com.g90.backend.modules.debt.dto.ReminderCreateRequest;
import com.g90.backend.modules.debt.dto.ReminderListQuery;
import com.g90.backend.modules.debt.dto.ReminderListResponseData;
import com.g90.backend.modules.debt.dto.ReminderSendResponseData;
import com.g90.backend.modules.debt.dto.SettlementConfirmRequest;
import com.g90.backend.modules.debt.dto.SettlementResponse;

public interface DebtService {

    DebtListResponseData getDebts(DebtListQuery query);

    DebtStatusResponseData getDebtStatus(String customerId);

    DebtAgingResponseData getDebtAging(String customerId);

    DebtHistoryResponseData getDebtHistory(String customerId);

    ReminderSendResponseData sendReminder(ReminderCreateRequest request);

    ReminderListResponseData getReminders(ReminderListQuery query);

    SettlementResponse confirmSettlement(String customerId, SettlementConfirmRequest request);

    DebtSettlementListResponseData getSettlements(String customerId);

    byte[] exportDebts(DebtListQuery query);
}
