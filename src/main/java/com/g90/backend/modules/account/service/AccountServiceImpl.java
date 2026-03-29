package com.g90.backend.modules.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.AccountNotFoundException;
import com.g90.backend.exception.EmailAlreadyExistsException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.dto.AccountCreateDataResponse;
import com.g90.backend.modules.account.dto.AccountCreateRequest;
import com.g90.backend.modules.account.dto.AccountDeactivateRequest;
import com.g90.backend.modules.account.dto.AccountDetailResponse;
import com.g90.backend.modules.account.dto.AccountListQuery;
import com.g90.backend.modules.account.dto.AccountListResponseData;
import com.g90.backend.modules.account.dto.AccountUpdateRequest;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.mapper.AccountMapper;
import com.g90.backend.modules.account.repository.AccountSpecifications;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.RoleRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Set<RoleName> ASSIGNABLE_ROLES = Set.of(RoleName.ACCOUNTANT, RoleName.WAREHOUSE);

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AccountCreateDataResponse createAccount(AccountCreateRequest request) {
        ensureEmailUnique(normalize(request.getEmail()));
        RoleEntity role = resolveAssignableRole(request.getRoleId());

        UserAccountEntity entity = accountMapper.toEntity(request, role, passwordEncoder.encode(request.getPassword()));
        entity.setFullName(normalize(request.getFullName()));
        entity.setEmail(normalize(request.getEmail()));
        entity.setPhone(normalizeNullable(request.getPhone()));
        entity.setAddress(normalizeNullable(request.getAddress()));
        entity.setStatus(AccountStatus.ACTIVE.name());

        UserAccountEntity saved = userAccountRepository.save(entity);
        logAudit("CREATE_USER", saved.getId(), null, Map.of(
                "id", saved.getId(),
                "email", saved.getEmail(),
                "role", saved.getRole().getName(),
                "status", saved.getStatus()
        ));

        return accountMapper.toCreateData(saved);
    }

    @Override
    @Transactional
    public AccountListResponseData getAccounts(AccountListQuery query) {
        normalizeAndValidateQuery(query);
        Page<UserAccountEntity> page = userAccountRepository.findAll(
                AccountSpecifications.withFilters(query),
                PageRequest.of(normalizePage(query.getPage()) - 1, normalizeSize(query.getPageSize()), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        logAudit("VIEW_USER_LIST", null, null, auditPayload(
                "page", query.getPage(),
                "pageSize", query.getPageSize(),
                "role", query.getRole(),
                "status", query.getStatus()
        ));
        return accountMapper.toListResponse(page);
    }

    @Override
    @Transactional
    public AccountDetailResponse getAccountById(String id) {
        UserAccountEntity account = findInternalAccount(id);
        AccountDetailResponse response = accountMapper.toDetailResponse(account);
        logAudit("VIEW_USER_DETAIL", account.getId(), null, response);
        return response;
    }

    @Override
    @Transactional
    public void updateAccount(String id, AccountUpdateRequest request) {
        UserAccountEntity account = findInternalAccount(id);
        AccountDetailResponse oldState = accountMapper.toDetailResponse(account);

        RoleEntity requestedRole = resolveRoleForUpdate(account, request.getRoleId());
        account.setFullName(normalize(request.getFullName()));
        account.setPhone(normalizeNullable(request.getPhone()));
        account.setAddress(normalizeNullable(request.getAddress()));
        account.setRole(requestedRole);
        account.setStatus(resolveStatus(request.getStatus()).name());

        UserAccountEntity saved = userAccountRepository.save(account);
        logAudit("UPDATE_USER", saved.getId(), oldState, accountMapper.toDetailResponse(saved));
    }

    @Override
    @Transactional
    public void deactivateAccount(String id, AccountDeactivateRequest request) {
        UserAccountEntity account = findInternalAccount(id);
        if (RoleName.OWNER.name().equalsIgnoreCase(account.getRole().getName())) {
            throw new ForbiddenOperationException("Permission denied");
        }

        AccountDetailResponse oldState = accountMapper.toDetailResponse(account);
        account.setStatus(AccountStatus.INACTIVE.name());
        UserAccountEntity saved = userAccountRepository.save(account);
        logAudit("DEACTIVATE_USER", saved.getId(), oldState, auditPayload(
                "id", saved.getId(),
                "status", saved.getStatus(),
                "reason", normalizeNullable(request.getReason())
        ));
    }

    private void ensureEmailUnique(String email) {
        Optional<UserAccountEntity> existing = userAccountRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            throw new EmailAlreadyExistsException();
        }
    }

    private UserAccountEntity findInternalAccount(String id) {
        UserAccountEntity account = userAccountRepository.findById(id).orElseThrow(AccountNotFoundException::new);
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(account.getRole().getName())) {
            throw new AccountNotFoundException();
        }
        return account;
    }

    private RoleEntity resolveAssignableRole(String roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> RequestValidationException.singleError("roleId", "Role is invalid"));
        RoleName roleName = RoleName.from(role.getName());
        if (!ASSIGNABLE_ROLES.contains(roleName)) {
            throw RequestValidationException.singleError("roleId", "Role must be ACCOUNTANT or WAREHOUSE");
        }
        return role;
    }

    private RoleEntity resolveRoleForUpdate(UserAccountEntity account, String roleId) {
        RoleEntity requestedRole = roleRepository.findById(roleId)
                .orElseThrow(() -> RequestValidationException.singleError("roleId", "Role is invalid"));

        if (RoleName.OWNER.name().equalsIgnoreCase(account.getRole().getName())
                && !account.getRole().getId().equals(requestedRole.getId())) {
            throw new ForbiddenOperationException("Owner cannot change own role");
        }

        if (!RoleName.OWNER.name().equalsIgnoreCase(account.getRole().getName())) {
            RoleName roleName = RoleName.from(requestedRole.getName());
            if (!ASSIGNABLE_ROLES.contains(roleName)) {
                throw RequestValidationException.singleError("roleId", "Role must be ACCOUNTANT or WAREHOUSE");
            }
        }

        return requestedRole;
    }

    private void normalizeAndValidateQuery(AccountListQuery query) {
        query.setRole(normalizeNullable(query.getRole()));
        query.setStatus(normalizeNullable(query.getStatus()));

        if (StringUtils.hasText(query.getRole())) {
            try {
                RoleName.from(query.getRole());
            } catch (IllegalArgumentException exception) {
                throw RequestValidationException.singleError("role", "Role is invalid");
            }
            query.setRole(query.getRole().toUpperCase());
        }

        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus()).name());
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 10 : size;
    }

    private AccountStatus resolveStatus(String status) {
        try {
            return AccountStatus.from(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RequestValidationException.singleError("status", "status must be ACTIVE, PENDING_VERIFICATION, INACTIVE, or LOCKED");
        }
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(null);
        auditLog.setAction(action);
        auditLog.setEntityType("USER");
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit payload", exception);
        }
    }

    private Map<String, Object> auditPayload(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Audit payload requires key/value pairs");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            payload.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return payload;
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
