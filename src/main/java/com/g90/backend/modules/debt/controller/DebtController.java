package com.g90.backend.modules.debt.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
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
import com.g90.backend.modules.debt.service.DebtService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debts")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class DebtController {

    private final DebtService debtService;

    @GetMapping
    public ApiResponse<DebtListResponseData> getDebts(@Valid @ModelAttribute DebtListQuery query) {
        return ApiResponse.success("Debt list fetched successfully", debtService.getDebts(query));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDebts(@Valid @ModelAttribute DebtListQuery query) {
        byte[] csv = debtService.exportDebts(query);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("debt-report.csv").build().toString())
                .body(csv);
    }

    @PostMapping("/reminders")
    public ApiResponse<ReminderSendResponseData> sendReminder(@Valid @RequestBody ReminderCreateRequest request) {
        return ApiResponse.success("Debt reminder sent successfully", debtService.sendReminder(request));
    }

    @GetMapping("/reminders")
    public ApiResponse<ReminderListResponseData> getReminders(@Valid @ModelAttribute ReminderListQuery query) {
        return ApiResponse.success("Debt reminder history fetched successfully", debtService.getReminders(query));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<DebtStatusResponseData> getDebtStatus(@PathVariable String customerId) {
        return ApiResponse.success("Debt status fetched successfully", debtService.getDebtStatus(customerId));
    }

    @GetMapping("/{customerId}/aging")
    public ApiResponse<DebtAgingResponseData> getDebtAging(@PathVariable String customerId) {
        return ApiResponse.success("Debt aging fetched successfully", debtService.getDebtAging(customerId));
    }

    @GetMapping("/{customerId}/history")
    public ApiResponse<DebtHistoryResponseData> getDebtHistory(@PathVariable String customerId) {
        return ApiResponse.success("Debt history fetched successfully", debtService.getDebtHistory(customerId));
    }

    @PostMapping("/{customerId}/settlement-confirmation")
    public ApiResponse<SettlementResponse> confirmSettlement(
            @PathVariable String customerId,
            @Valid @RequestBody SettlementConfirmRequest request
    ) {
        return ApiResponse.success("Debt settlement confirmed successfully", debtService.confirmSettlement(customerId, request));
    }

    @GetMapping("/{customerId}/settlements")
    public ApiResponse<DebtSettlementListResponseData> getSettlements(@PathVariable String customerId) {
        return ApiResponse.success("Debt settlements fetched successfully", debtService.getSettlements(customerId));
    }
}
