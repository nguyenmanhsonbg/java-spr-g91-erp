package com.g90.backend.modules.account.service;

import com.g90.backend.modules.account.dto.AccountCreateDataResponse;
import com.g90.backend.modules.account.dto.AccountCreateRequest;
import com.g90.backend.modules.account.dto.AccountDeactivateRequest;
import com.g90.backend.modules.account.dto.AccountDetailResponse;
import com.g90.backend.modules.account.dto.AccountListQuery;
import com.g90.backend.modules.account.dto.AccountListResponseData;
import com.g90.backend.modules.account.dto.AccountUpdateRequest;

public interface AccountService {

    AccountCreateDataResponse createAccount(AccountCreateRequest request);

    AccountListResponseData getAccounts(AccountListQuery query);

    AccountDetailResponse getAccountById(String id);

    void updateAccount(String id, AccountUpdateRequest request);

    void deactivateAccount(String id, AccountDeactivateRequest request);
}
