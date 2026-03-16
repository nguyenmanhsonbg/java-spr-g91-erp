package com.g90.backend.modules.customer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerAlreadyInactiveException;
import com.g90.backend.exception.CustomerDisableNotAllowedException;
import com.g90.backend.exception.CustomerNotFoundException;
import com.g90.backend.exception.DuplicateCustomerTaxCodeException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.exception.SystemRoleNotConfiguredException;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.RoleRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.dto.CustomerCreateRequest;
import com.g90.backend.modules.customer.dto.CustomerCreateResponse;
import com.g90.backend.modules.customer.dto.CustomerDetailResponseData;
import com.g90.backend.modules.customer.dto.CustomerDisableRequest;
import com.g90.backend.modules.customer.dto.CustomerListQuery;
import com.g90.backend.modules.customer.dto.CustomerListResponseData;
import com.g90.backend.modules.customer.dto.CustomerStatusResponse;
import com.g90.backend.modules.customer.dto.CustomerSummaryResponseData;
import com.g90.backend.modules.customer.dto.CustomerUpdateRequest;
import com.g90.backend.modules.customer.entity.CustomerStatus;
import com.g90.backend.modules.customer.entity.CustomerType;
import com.g90.backend.modules.customer.mapper.CustomerMapper;
import com.g90.backend.modules.customer.repository.CustomerAnalyticsRepository;
import com.g90.backend.modules.customer.repository.CustomerManagementRepository;
import com.g90.backend.modules.customer.repository.CustomerSpecifications;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class CustomerServiceImpl implements CustomerService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "customerCode", "companyName", "creditLimit");
    private static final Set<String> SORT_DIRECTIONS = Set.of("asc", "desc");
    private static final BigDecimal DISABLE_DEBT_THRESHOLD = new BigDecimal("100000.00");
    private static final String DEFAULT_PAYMENT_TERMS = "70% on delivery, 30% within 30 days";
    private static final String PASSWORD_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";

    private final CustomerManagementRepository customerManagementRepository;
    private final CustomerAnalyticsRepository customerAnalyticsRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CustomerCreateResponse createCustomer(CustomerCreateRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        normalizeAndValidateCreateRequest(request);
        assertUniqueTaxCode(request.getTaxCode(), null);

        String temporaryPassword = null;
        UserAccountEntity portalAccount = null;
        if (Boolean.TRUE.equals(request.getCreatePortalAccount())) {
            if (!StringUtils.hasText(request.getEmail())) {
                throw RequestValidationException.singleError("email", "Email is required when creating portal account");
            }
            assertUniqueUserEmail(request.getEmail(), null);
            temporaryPassword = generateTemporaryPassword();
            portalAccount = createPortalAccount(request, temporaryPassword);
        }

        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setCustomerCode(generateCustomerCode());
        customer.setUser(portalAccount);
        applyCustomerData(
                customer,
                request.getCompanyName(),
                request.getTaxCode(),
                request.getAddress(),
                request.getContactPerson(),
                request.getPhone(),
                request.getEmail(),
                request.getCustomerType(),
                request.getPriceGroup(),
                request.getCreditLimit(),
                request.getPaymentTerms()
        );
        customer.setStatus(CustomerStatus.ACTIVE.name());

        CustomerProfileEntity saved = customerManagementRepository.save(customer);
        CustomerCreateResponse response = customerMapper.toCreateResponse(saved, temporaryPassword);
        logAudit("CREATE_CUSTOMER", saved.getId(), null, sanitizeCreateResponse(response), currentUser.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerListResponseData getCustomers(CustomerListQuery query) {
        requireAccountantAccess();
        normalizeAndValidateQuery(query);
        Page<CustomerProfileEntity> page = customerManagementRepository.findAll(
                CustomerSpecifications.withFilters(query),
                PageRequest.of(query.getPage() - 1, query.getPageSize(), buildSort(query))
        );
        return customerMapper.toListResponse(page, query);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponseData getCustomerDetail(String id) {
        requireAccountantAccess();
        CustomerProfileEntity customer = findCustomer(id);
        return customerMapper.toDetailResponse(
                customer,
                customerAnalyticsRepository.getAggregateSnapshot(customer.getId()),
                customerAnalyticsRepository.getRecentTransactions(customer.getId(), 10)
        );
    }

    @Override
    @Transactional
    public CustomerDetailResponseData updateCustomer(String id, CustomerUpdateRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        normalizeAndValidateUpdateRequest(request);

        CustomerProfileEntity customer = findCustomer(id);
        CustomerDetailResponseData oldState = customerMapper.toDetailResponse(
                customer,
                customerAnalyticsRepository.getAggregateSnapshot(customer.getId()),
                customerAnalyticsRepository.getRecentTransactions(customer.getId(), 10)
        );

        assertUniqueTaxCode(request.getTaxCode(), customer.getId());
        if (customer.getUser() != null && StringUtils.hasText(request.getEmail())) {
            assertUniqueUserEmail(request.getEmail(), customer.getUser().getId());
        }

        applyCustomerData(
                customer,
                request.getCompanyName(),
                request.getTaxCode(),
                request.getAddress(),
                request.getContactPerson(),
                request.getPhone(),
                request.getEmail(),
                request.getCustomerType(),
                request.getPriceGroup(),
                request.getCreditLimit(),
                request.getPaymentTerms()
        );
        syncLinkedUser(customer);

        CustomerProfileEntity saved = customerManagementRepository.save(customer);
        CustomerDetailResponseData response = customerMapper.toDetailResponse(
                saved,
                customerAnalyticsRepository.getAggregateSnapshot(saved.getId()),
                customerAnalyticsRepository.getRecentTransactions(saved.getId(), 10)
        );
        logAudit("UPDATE_CUSTOMER", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional
    public CustomerStatusResponse disableCustomer(String id, CustomerDisableRequest request) {
        UserAccountEntity currentUser = requireAccountantAccess();
        CustomerProfileEntity customer = findCustomer(id);
        if (CustomerStatus.INACTIVE.name().equalsIgnoreCase(customer.getStatus())) {
            throw new CustomerAlreadyInactiveException();
        }

        List<String> blockers = buildDisableBlockers(customer);
        if (!blockers.isEmpty()) {
            throw new CustomerDisableNotAllowedException(String.join("; ", blockers));
        }

        CustomerStatusResponse oldState = customerMapper.toStatusResponse(customer, null);
        customer.setStatus(CustomerStatus.INACTIVE.name());
        if (customer.getUser() != null) {
            customer.getUser().setStatus(AccountStatus.INACTIVE.name());
            userAccountRepository.save(customer.getUser());
        }

        CustomerProfileEntity saved = customerManagementRepository.save(customer);
        CustomerStatusResponse response = customerMapper.toStatusResponse(saved, request.getReason().trim());
        logAudit("DISABLE_CUSTOMER", saved.getId(), oldState, response, currentUser.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSummaryResponseData getCustomerSummary(String id) {
        requireAccountantAccess();
        CustomerProfileEntity customer = findCustomer(id);
        return customerMapper.toSummaryResponse(
                customer,
                customerAnalyticsRepository.getAggregateSnapshot(customer.getId()),
                buildDisableBlockers(customer)
        );
    }

    private void applyCustomerData(
            CustomerProfileEntity customer,
            String companyName,
            String taxCode,
            String address,
            String contactPerson,
            String phone,
            String email,
            String customerType,
            String priceGroup,
            BigDecimal creditLimit,
            String paymentTerms
    ) {
        customer.setCompanyName(normalizeRequired(companyName, "companyName", "Company name is required"));
        customer.setTaxCode(normalizeRequired(taxCode, "taxCode", "Tax code is required"));
        customer.setAddress(normalizeNullable(address));
        customer.setContactPerson(normalizeNullable(contactPerson));
        customer.setPhone(normalizeNullable(phone));
        customer.setEmail(normalizeNullableEmail(email));
        customer.setCustomerType(resolveCustomerType(customerType, "customerType"));
        customer.setPriceGroup(resolveCustomerType(StringUtils.hasText(priceGroup) ? priceGroup : customerType, "priceGroup"));
        customer.setCreditLimit(normalizeMoney(creditLimit));
        customer.setPaymentTerms(StringUtils.hasText(paymentTerms) ? paymentTerms.trim() : DEFAULT_PAYMENT_TERMS);
    }

    private void syncLinkedUser(CustomerProfileEntity customer) {
        if (customer.getUser() == null) {
            return;
        }
        customer.getUser().setFullName(resolvePortalFullName(customer));
        customer.getUser().setEmail(customer.getEmail());
        customer.getUser().setPhone(customer.getPhone());
        customer.getUser().setAddress(customer.getAddress());
        userAccountRepository.save(customer.getUser());
    }

    private UserAccountEntity createPortalAccount(CustomerCreateRequest request, String temporaryPassword) {
        RoleEntity customerRole = roleRepository.findByNameIgnoreCase(RoleName.CUSTOMER.name())
                .orElseThrow(() -> new SystemRoleNotConfiguredException(RoleName.CUSTOMER.name()));

        UserAccountEntity user = new UserAccountEntity();
        user.setRole(customerRole);
        user.setFullName(resolvePortalFullName(request.getContactPerson(), request.getCompanyName()));
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setAddress(normalizeNullable(request.getAddress()));
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        return userAccountRepository.save(user);
    }

    private String resolvePortalFullName(CustomerProfileEntity customer) {
        return resolvePortalFullName(customer.getContactPerson(), customer.getCompanyName());
    }

    private String resolvePortalFullName(String contactPerson, String companyName) {
        if (StringUtils.hasText(contactPerson)) {
            return contactPerson.trim();
        }
        return companyName.trim();
    }

    private CustomerProfileEntity findCustomer(String id) {
        return customerManagementRepository.findDetailedById(id).orElseThrow(CustomerNotFoundException::new);
    }

    private void assertUniqueTaxCode(String taxCode, String currentCustomerId) {
        Optional<CustomerProfileEntity> existingCustomer = customerManagementRepository.findByTaxCodeIgnoreCase(taxCode.trim());
        if (existingCustomer.isPresent() && !existingCustomer.get().getId().equals(currentCustomerId)) {
            throw new DuplicateCustomerTaxCodeException();
        }
    }

    private void assertUniqueUserEmail(String email, String currentUserId) {
        Optional<UserAccountEntity> existingUser = userAccountRepository.findByEmailIgnoreCase(email.trim());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId)) {
            throw RequestValidationException.singleError("email", "Email is already used by another portal account");
        }
    }

    private String generateCustomerCode() {
        long nextSequence = Optional.ofNullable(customerManagementRepository.findMaxCustomerCodeSequence()).orElse(0L) + 1L;
        return "CUST-" + String.format(Locale.ROOT, "%03d", nextSequence);
    }

    private List<String> buildDisableBlockers(CustomerProfileEntity customer) {
        CustomerAnalyticsRepository.CustomerAggregateSnapshot snapshot = customerAnalyticsRepository.getAggregateSnapshot(customer.getId());
        List<String> blockers = new ArrayList<>();
        BigDecimal outstandingDebt = normalizeMoney(snapshot.totalInvoicedAmount()).subtract(normalizeMoney(snapshot.totalAllocatedPayments()));
        if (outstandingDebt.compareTo(DISABLE_DEBT_THRESHOLD) > 0) {
            blockers.add("Outstanding debt exceeds 100,000 VND");
        }
        if (snapshot.activeProjectCount() > 0) {
            blockers.add("Customer still has active projects");
        }
        if (snapshot.openContractCount() > 0) {
            blockers.add("Customer still has open contracts/orders");
        }
        if (CustomerStatus.INACTIVE.name().equalsIgnoreCase(customer.getStatus())) {
            blockers.add("Customer is already inactive");
        }
        return blockers;
    }

    private UserAccountEntity requireAccountantAccess() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        UserAccountEntity user = userAccountRepository.findWithRoleById(currentUser.userId())
                .orElseThrow(() -> new ForbiddenOperationException("You do not have permission to perform this action"));
        if (RoleName.from(user.getRole().getName()) != RoleName.ACCOUNTANT) {
            throw new ForbiddenOperationException("Only accountant users can perform this action");
        }
        return user;
    }

    private void normalizeAndValidateCreateRequest(CustomerCreateRequest request) {
        request.setCompanyName(normalizeRequired(request.getCompanyName(), "companyName", "Company name is required"));
        request.setTaxCode(normalizeRequired(request.getTaxCode(), "taxCode", "Tax code is required"));
        request.setAddress(normalizeNullable(request.getAddress()));
        request.setContactPerson(normalizeNullable(request.getContactPerson()));
        request.setPhone(normalizeNullable(request.getPhone()));
        request.setEmail(normalizeNullableEmail(request.getEmail()));
        request.setCustomerType(resolveCustomerType(request.getCustomerType(), "customerType"));
        request.setPriceGroup(resolveCustomerType(StringUtils.hasText(request.getPriceGroup()) ? request.getPriceGroup() : request.getCustomerType(), "priceGroup"));
        request.setCreditLimit(normalizeMoney(request.getCreditLimit()));
        request.setPaymentTerms(StringUtils.hasText(request.getPaymentTerms()) ? request.getPaymentTerms().trim() : DEFAULT_PAYMENT_TERMS);
    }

    private void normalizeAndValidateUpdateRequest(CustomerUpdateRequest request) {
        request.setCompanyName(normalizeRequired(request.getCompanyName(), "companyName", "Company name is required"));
        request.setTaxCode(normalizeRequired(request.getTaxCode(), "taxCode", "Tax code is required"));
        request.setAddress(normalizeNullable(request.getAddress()));
        request.setContactPerson(normalizeNullable(request.getContactPerson()));
        request.setPhone(normalizeNullable(request.getPhone()));
        request.setEmail(normalizeNullableEmail(request.getEmail()));
        request.setCustomerType(resolveCustomerType(request.getCustomerType(), "customerType"));
        request.setPriceGroup(resolveCustomerType(StringUtils.hasText(request.getPriceGroup()) ? request.getPriceGroup() : request.getCustomerType(), "priceGroup"));
        request.setCreditLimit(normalizeMoney(request.getCreditLimit()));
        request.setPaymentTerms(StringUtils.hasText(request.getPaymentTerms()) ? request.getPaymentTerms().trim() : DEFAULT_PAYMENT_TERMS);
        request.setChangeReason(normalizeNullable(request.getChangeReason()));
    }

    private void normalizeAndValidateQuery(CustomerListQuery query) {
        query.setKeyword(normalizeNullable(query.getKeyword()));
        query.setCustomerCode(normalizeNullable(query.getCustomerCode()));
        query.setTaxCode(normalizeNullable(query.getTaxCode()));
        query.setCustomerType(normalizeNullable(query.getCustomerType()));
        query.setPriceGroup(normalizeNullable(query.getPriceGroup()));
        query.setStatus(normalizeNullable(query.getStatus()));
        query.setSortBy(StringUtils.hasText(query.getSortBy()) ? query.getSortBy().trim() : "createdAt");
        query.setSortDir(StringUtils.hasText(query.getSortDir()) ? query.getSortDir().trim().toLowerCase(Locale.ROOT) : "desc");

        if (query.getCreatedFrom() != null && query.getCreatedTo() != null && query.getCreatedFrom().isAfter(query.getCreatedTo())) {
            throw RequestValidationException.singleError("createdFrom", "createdFrom must be before or equal to createdTo");
        }
        if (StringUtils.hasText(query.getCustomerType())) {
            query.setCustomerType(resolveCustomerType(query.getCustomerType(), "customerType"));
        }
        if (StringUtils.hasText(query.getPriceGroup())) {
            query.setPriceGroup(resolveCustomerType(query.getPriceGroup(), "priceGroup"));
        }
        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(resolveStatus(query.getStatus()));
        }
        if (!SORT_FIELDS.contains(query.getSortBy())) {
            throw RequestValidationException.singleError("sortBy", "sortBy must be one of createdAt, customerCode, companyName, creditLimit");
        }
        if (!SORT_DIRECTIONS.contains(query.getSortDir())) {
            throw RequestValidationException.singleError("sortDir", "sortDir must be asc or desc");
        }
    }

    private Sort buildSort(CustomerListQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, query.getSortBy());
    }

    private String resolveCustomerType(String value, String field) {
        try {
            return CustomerType.from(value).name();
        } catch (Exception exception) {
            throw RequestValidationException.singleError(field, field + " must be RETAIL, CONTRACTOR, or DISTRIBUTOR");
        }
    }

    private String resolveStatus(String value) {
        try {
            return CustomerStatus.from(value).name();
        } catch (Exception exception) {
            throw RequestValidationException.singleError("status", "status must be ACTIVE or INACTIVE");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw RequestValidationException.singleError(field, message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeNullableEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(PASSWORD_CHARSET.charAt(random.nextInt(PASSWORD_CHARSET.length())));
        }
        return builder.toString();
    }

    private Object sanitizeCreateResponse(CustomerCreateResponse response) {
        if (response.portalAccount() == null) {
            return response;
        }
        return new CustomerCreateResponse(
                response.id(),
                response.customerCode(),
                response.companyName(),
                response.status(),
                new CustomerCreateResponse.PortalAccountData(
                        response.portalAccount().userId(),
                        response.portalAccount().email(),
                        null
                )
        );
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("CUSTOMER");
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
}
