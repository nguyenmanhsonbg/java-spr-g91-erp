package com.g90.backend.modules.account.mapper;

import com.g90.backend.modules.account.dto.AccountCreateDataResponse;
import com.g90.backend.modules.account.dto.AccountCreateRequest;
import com.g90.backend.modules.account.dto.AccountDetailResponse;
import com.g90.backend.modules.account.dto.AccountListItemResponse;
import com.g90.backend.modules.account.dto.AccountListResponseData;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public UserAccountEntity toEntity(AccountCreateRequest request, RoleEntity role, String passwordHash) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setRole(role);
        entity.setFullName(request.getFullName());
        entity.setEmail(request.getEmail());
        entity.setPasswordHash(passwordHash);
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        return entity;
    }

    public AccountCreateDataResponse toCreateData(UserAccountEntity entity) {
        return AccountCreateDataResponse.builder()
                .id(entity.getId())
                .build();
    }

    public AccountListItemResponse toListItem(UserAccountEntity entity) {
        return AccountListItemResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole().getName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AccountListResponseData toListResponse(Page<UserAccountEntity> page) {
        return AccountListResponseData.builder()
                .content(page.getContent().stream().map(this::toListItem).toList())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .build();
    }

    public AccountDetailResponse toDetailResponse(UserAccountEntity entity) {
        return AccountDetailResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .role(entity.getRole().getName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
