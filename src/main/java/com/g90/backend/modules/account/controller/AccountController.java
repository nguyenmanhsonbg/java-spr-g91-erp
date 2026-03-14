package com.g90.backend.modules.account.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.account.dto.AccountCreateDataResponse;
import com.g90.backend.modules.account.dto.AccountCreateRequest;
import com.g90.backend.modules.account.dto.AccountDeactivateRequest;
import com.g90.backend.modules.account.dto.AccountDetailResponse;
import com.g90.backend.modules.account.dto.AccountListQuery;
import com.g90.backend.modules.account.dto.AccountListResponseData;
import com.g90.backend.modules.account.dto.AccountUpdateRequest;
import com.g90.backend.modules.account.service.AccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountCreateDataResponse>> createAccount(
            @Valid @RequestBody AccountCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User account created successfully", accountService.createAccount(request)));
    }

    @GetMapping
    public ApiResponse<AccountListResponseData> getAccounts(@Valid @ModelAttribute AccountListQuery query) {
        return ApiResponse.success("User list fetched successfully", accountService.getAccounts(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountDetailResponse> getAccountById(@PathVariable String id) {
        return ApiResponse.success("User detail fetched successfully", accountService.getAccountById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        accountService.updateAccount(id, request);
        return ApiResponse.success("User account updated successfully", null);
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivateAccount(
            @PathVariable String id,
            @RequestBody(required = false) AccountDeactivateRequest request
    ) {
        accountService.deactivateAccount(id, request == null ? new AccountDeactivateRequest() : request);
        return ApiResponse.success("User account deactivated successfully", null);
    }
}
