package com.g90.backend.modules.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.CustomerDisableNotAllowedException;
import com.g90.backend.exception.DuplicateCustomerTaxCodeException;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.customer.entity.CustomerPriceGroup;
import com.g90.backend.modules.account.repository.RoleRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.dto.CustomerCreateRequest;
import com.g90.backend.modules.customer.dto.CustomerDisableRequest;
import com.g90.backend.modules.customer.dto.CustomerUpdateRequest;
import com.g90.backend.modules.customer.mapper.CustomerMapper;
import com.g90.backend.modules.customer.repository.CustomerAnalyticsRepository;
import com.g90.backend.modules.customer.repository.CustomerManagementRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerServiceImplTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private CustomerManagementRepository customerManagementRepository;
    @Mock
    private CustomerAnalyticsRepository customerAnalyticsRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private final CustomerMapper customerMapper = new CustomerMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                customerManagementRepository,
                customerAnalyticsRepository,
                userAccountRepository,
                roleRepository,
                auditLogRepository,
                currentUserProvider,
                passwordEncoder,
                customerMapper,
                objectMapper
        );
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerAnalyticsRepository.getRecentTransactions(any(), any(Integer.class))).thenReturn(List.of());
        when(customerAnalyticsRepository.getAggregateSnapshot(any())).thenReturn(snapshot(BigDecimal.ZERO, 0, 0));
    }

    @Test
    void createCustomerSuccess() {
        authenticateAsAccountant();
        when(customerManagementRepository.findByTaxCodeIgnoreCase("0101234567")).thenReturn(Optional.empty());
        when(customerManagementRepository.findMaxCustomerCodeSequence()).thenReturn(7L);
        when(customerManagementRepository.save(any())).thenAnswer(invocation -> {
            CustomerProfileEntity customer = invocation.getArgument(0);
            customer.setId("customer-1");
            customer.setCreatedAt(LocalDateTime.now(APP_ZONE));
            customer.setUpdatedAt(LocalDateTime.now(APP_ZONE));
            return customer;
        });

        var response = customerService.createCustomer(createRequest());

        assertThat(response.id()).isEqualTo("customer-1");
        assertThat(response.customerCode()).isEqualTo("CUST-008");
        assertThat(response.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<CustomerProfileEntity> customerCaptor = ArgumentCaptor.forClass(CustomerProfileEntity.class);
        verify(customerManagementRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getPriceGroup()).isEqualTo(CustomerPriceGroup.DEFAULT);
    }

    @Test
    void createCustomerFailsWhenTaxCodeDuplicated() {
        authenticateAsAccountant();
        when(customerManagementRepository.findByTaxCodeIgnoreCase("0101234567")).thenReturn(Optional.of(customer("customer-1")));

        assertThatThrownBy(() -> customerService.createCustomer(createRequest()))
                .isInstanceOf(DuplicateCustomerTaxCodeException.class);
    }

    @Test
    void createCustomerWithPortalAccountSuccess() {
        authenticateAsAccountant();
        UserAccountEntity createdUser = customerUser();
        createdUser.setId("user-customer-1");
        createdUser.setEmail("contact@abcsteel.vn");

        when(customerManagementRepository.findByTaxCodeIgnoreCase("0101234567")).thenReturn(Optional.empty());
        when(customerManagementRepository.findMaxCustomerCodeSequence()).thenReturn(1L);
        when(roleRepository.findByNameIgnoreCase(RoleName.CUSTOMER.name())).thenReturn(Optional.of(role(RoleName.CUSTOMER)));
        when(userAccountRepository.findByEmailIgnoreCase("contact@abcsteel.vn")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userAccountRepository.save(any())).thenReturn(createdUser);
        when(customerManagementRepository.save(any())).thenAnswer(invocation -> {
            CustomerProfileEntity customer = invocation.getArgument(0);
            customer.setId("customer-portal");
            customer.setCreatedAt(LocalDateTime.now(APP_ZONE));
            customer.setUpdatedAt(LocalDateTime.now(APP_ZONE));
            return customer;
        });

        CustomerCreateRequest request = createRequest();
        request.setCreatePortalAccount(true);

        var response = customerService.createCustomer(request);

        assertThat(response.portalAccount()).isNotNull();
        assertThat(response.portalAccount().userId()).isEqualTo("user-customer-1");
        assertThat(response.portalAccount().temporaryPassword()).isNotBlank();
    }

    @Test
    void updateCustomerSuccess() {
        authenticateAsAccountant();
        CustomerProfileEntity customer = customer("customer-1");
        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer));
        when(customerManagementRepository.findByTaxCodeIgnoreCase("0101234568")).thenReturn(Optional.empty());
        when(customerManagementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerUpdateRequest request = updateRequest();
        request.setTaxCode("0101234568");
        request.setCompanyName("ABC Steel Construction JSC");

        var response = customerService.updateCustomer("customer-1", request);

        assertThat(response.customer().companyName()).isEqualTo("ABC Steel Construction JSC");
        assertThat(response.customer().taxCode()).isEqualTo("0101234568");
    }

    @Test
    void updateCustomerKeepsExistingPriceGroupWhenRequestOmitsIt() {
        authenticateAsAccountant();
        CustomerProfileEntity customer = customer("customer-1");
        customer.setPriceGroup(CustomerPriceGroup.DEFAULT);

        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer));
        when(customerManagementRepository.findByTaxCodeIgnoreCase("0101234567")).thenReturn(Optional.empty());
        when(customerManagementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerUpdateRequest request = updateRequest();
        request.setPriceGroup(null);

        var response = customerService.updateCustomer("customer-1", request);

        assertThat(response.customer().priceGroup()).isEqualTo(CustomerPriceGroup.DEFAULT);
    }

    @Test
    void disableCustomerSuccess() {
        authenticateAsAccountant();
        CustomerProfileEntity customer = customer("customer-1");
        customer.setUser(customerUser());
        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer));
        when(customerManagementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerAnalyticsRepository.getAggregateSnapshot("customer-1")).thenReturn(snapshot(new BigDecimal("50000.00"), 0, 0));

        CustomerDisableRequest request = new CustomerDisableRequest();
        request.setReason("Customer inactive for long period");

        var response = customerService.disableCustomer("customer-1", request);

        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(customer.getUser().getStatus()).isEqualTo(AccountStatus.INACTIVE.name());
        verify(userAccountRepository).save(customer.getUser());
    }

    @Test
    void disableCustomerFailsWhenDebtExists() {
        authenticateAsAccountant();
        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer("customer-1")));
        when(customerAnalyticsRepository.getAggregateSnapshot("customer-1")).thenReturn(snapshot(new BigDecimal("100001.00"), 0, 0));

        CustomerDisableRequest request = new CustomerDisableRequest();
        request.setReason("Cleanup");

        assertThatThrownBy(() -> customerService.disableCustomer("customer-1", request))
                .isInstanceOf(CustomerDisableNotAllowedException.class)
                .hasMessageContaining("Outstanding debt exceeds 100,000 VND");
    }

    @Test
    void disableCustomerFailsWhenActiveProjectsExist() {
        authenticateAsAccountant();
        when(customerManagementRepository.findDetailedById("customer-1")).thenReturn(Optional.of(customer("customer-1")));
        when(customerAnalyticsRepository.getAggregateSnapshot("customer-1")).thenReturn(snapshot(BigDecimal.ZERO, 2, 0));

        CustomerDisableRequest request = new CustomerDisableRequest();
        request.setReason("Cleanup");

        assertThatThrownBy(() -> customerService.disableCustomer("customer-1", request))
                .isInstanceOf(CustomerDisableNotAllowedException.class)
                .hasMessageContaining("Customer still has active projects");
    }

    private void authenticateAsAccountant() {
        UserAccountEntity accountant = accountantUser();
        when(currentUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(accountant.getId(), accountant.getEmail(), accountant.getRole().getName(), "token"));
        when(userAccountRepository.findWithRoleById(accountant.getId())).thenReturn(Optional.of(accountant));
    }

    private CustomerAnalyticsRepository.CustomerAggregateSnapshot snapshot(BigDecimal outstandingDebt, long activeProjectCount, long openContractCount) {
        return new CustomerAnalyticsRepository.CustomerAggregateSnapshot(
                2,
                1,
                1,
                1,
                activeProjectCount,
                openContractCount,
                outstandingDebt,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                LocalDateTime.now(APP_ZONE)
        );
    }

    private CustomerCreateRequest createRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCompanyName("ABC Steel Construction Co., Ltd");
        request.setTaxCode("0101234567");
        request.setAddress("Ha Noi");
        request.setContactPerson("Nguyen Van A");
        request.setPhone("0901234567");
        request.setEmail("contact@abcsteel.vn");
        request.setCustomerType("CONTRACTOR");
        request.setPriceGroup("CONTRACTOR");
        request.setCreditLimit(new BigDecimal("500000000.00"));
        request.setPaymentTerms("70% on delivery, 30% within 30 days");
        return request;
    }

    private CustomerUpdateRequest updateRequest() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setCompanyName("ABC Steel Construction Co., Ltd");
        request.setTaxCode("0101234567");
        request.setAddress("Ho Chi Minh");
        request.setContactPerson("Tran Van B");
        request.setPhone("0908888888");
        request.setEmail("newcontact@abcsteel.vn");
        request.setCustomerType("DISTRIBUTOR");
        request.setPriceGroup("DISTRIBUTOR-VIP");
        request.setCreditLimit(new BigDecimal("800000000.00"));
        request.setPaymentTerms("50% advance, 50% after delivery");
        request.setChangeReason("Update business profile");
        return request;
    }

    private CustomerProfileEntity customer(String id) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(id);
        customer.setCustomerCode("CUST-001");
        customer.setCompanyName("Customer " + id);
        customer.setTaxCode("0101234567");
        customer.setAddress("Ha Noi");
        customer.setContactPerson("Nguyen Van A");
        customer.setPhone("0901234567");
        customer.setEmail("contact@abcsteel.vn");
        customer.setCustomerType("CONTRACTOR");
        customer.setPriceGroup(CustomerPriceGroup.DEFAULT);
        customer.setCreditLimit(new BigDecimal("500000000.00"));
        customer.setPaymentTerms("70% on delivery, 30% within 30 days");
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(LocalDateTime.now(APP_ZONE).minusMonths(2));
        customer.setUpdatedAt(LocalDateTime.now(APP_ZONE));
        return customer;
    }

    private UserAccountEntity accountantUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-accountant-1");
        user.setEmail("accountant@example.com");
        user.setRole(role(RoleName.ACCOUNTANT));
        user.setStatus(AccountStatus.ACTIVE.name());
        return user;
    }

    private UserAccountEntity customerUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId("user-customer-1");
        user.setEmail("contact@abcsteel.vn");
        user.setRole(role(RoleName.CUSTOMER));
        user.setStatus(AccountStatus.ACTIVE.name());
        return user;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
