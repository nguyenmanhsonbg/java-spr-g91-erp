package com.g90.backend.modules.quotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.promotion.repository.PromotionRepository;
import com.g90.backend.modules.quotation.dto.QuotationManagementListQuery;
import com.g90.backend.modules.quotation.dto.QuotationSubmitRequest;
import com.g90.backend.modules.quotation.entity.ProjectEntity;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import com.g90.backend.modules.quotation.entity.QuotationItemEntity;
import com.g90.backend.modules.quotation.entity.QuotationStatus;
import com.g90.backend.modules.quotation.mapper.QuotationMapper;
import com.g90.backend.modules.quotation.repository.ProjectRepository;
import com.g90.backend.modules.quotation.repository.QuotationRepository;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotationServiceImplTest {

    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final QuotationMapper quotationMapper = new QuotationMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private QuotationServiceImpl quotationService;

    @BeforeEach
    void setUp() {
        quotationService = new QuotationServiceImpl(
                quotationRepository,
                projectRepository,
                productRepository,
                priceListRepository,
                promotionRepository,
                customerProfileRepository,
                userAccountRepository,
                auditLogRepository,
                contractRepository,
                quotationMapper,
                currentUserProvider,
                objectMapper
        );

        when(contractRepository.existsByQuotation_Id(anyString())).thenReturn(false);
    }

    @Test
    void getQuotationsAllowsAccountant() {
        authenticateAs(RoleName.ACCOUNTANT, "accountant-1");
        QuotationEntity quotation = quotation("quotation-1");

        when(quotationRepository.findAll(org.mockito.ArgumentMatchers.<Specification<QuotationEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(quotation), PageRequest.of(0, 20), 1));

        var response = quotationService.getQuotations(new QuotationManagementListQuery());

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quotationNumber()).isEqualTo("QT-20260316-0001");
        assertThat(response.items().get(0).customerName()).isEqualTo("Customer A");
        assertThat(response.items().get(0).canCreateContract()).isTrue();
    }

    @Test
    void getQuotationDetailAllowsAccountant() {
        authenticateAs(RoleName.ACCOUNTANT, "accountant-1");
        QuotationEntity quotation = quotation("quotation-1");

        when(quotationRepository.findDetailedById("quotation-1")).thenReturn(Optional.of(quotation));

        var response = quotationService.getQuotationDetail("quotation-1");

        assertThat(response.quotation().id()).isEqualTo("quotation-1");
        assertThat(response.customer().companyName()).isEqualTo("Customer A");
        assertThat(response.actions().accountantCanCreateContract()).isTrue();
    }

    @Test
    void getQuotationPreviewAllowsAccountant() {
        authenticateAs(RoleName.ACCOUNTANT, "accountant-1");
        QuotationEntity quotation = quotation("quotation-1");

        when(quotationRepository.findDetailedById("quotation-1")).thenReturn(Optional.of(quotation));

        var response = quotationService.getQuotationPreview("quotation-1");

        assertThat(response.quotation().id()).isEqualTo("quotation-1");
        assertThat(response.items()).hasSize(1);
        assertThat(response.summary().totalAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void previewQuotationRejectsClientProvidedUnitPrice() throws Exception {
        authenticateAs(RoleName.CUSTOMER, "customer-user");
        UserAccountEntity user = user("customer-user", RoleName.CUSTOMER);
        CustomerProfileEntity customer = customer("customer-1", "Customer A");

        when(userAccountRepository.findWithRoleById("customer-user")).thenReturn(Optional.of(user));
        when(customerProfileRepository.findByUser_Id("customer-user")).thenReturn(Optional.of(customer));

        QuotationSubmitRequest request = objectMapper.readValue("""
                {
                  "items": [
                    {
                      "productId": "product-1",
                      "quantity": 10,
                      "unitPrice": 10000
                    }
                  ]
                }
                """, QuotationSubmitRequest.class);

        assertThatThrownBy(() -> quotationService.previewQuotation(request))
                .isInstanceOf(RequestValidationException.class)
                .hasMessageContaining("Invalid request data");
    }

    private void authenticateAs(RoleName roleName, String userId) {
        when(currentUserProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser(userId, userId + "@g90steel.vn", roleName.name(), "token")
        );
    }

    private QuotationEntity quotation(String quotationId) {
        CustomerProfileEntity customer = customer("customer-1", "Customer A");
        ProductEntity product = product("product-1", "SP001");
        ProjectEntity project = project("project-1", customer);

        QuotationEntity quotation = new QuotationEntity();
        quotation.setId(quotationId);
        quotation.setQuotationNumber("QT-20260316-0001");
        quotation.setCustomer(customer);
        quotation.setProject(project);
        quotation.setStatus(QuotationStatus.PENDING.name());
        quotation.setTotalAmount(new BigDecimal("10000.00"));
        quotation.setValidUntil(LocalDate.of(2026, 3, 31));
        quotation.setCreatedAt(LocalDateTime.of(2026, 3, 16, 9, 0));
        quotation.setDeliveryRequirement("Deliver before 5PM");
        quotation.setItems(new ArrayList<>(List.of(quotationItem("item-1", quotation, product))));
        return quotation;
    }

    private QuotationItemEntity quotationItem(String itemId, QuotationEntity quotation, ProductEntity product) {
        QuotationItemEntity item = new QuotationItemEntity();
        item.setId(itemId);
        item.setQuotation(quotation);
        item.setProduct(product);
        item.setQuantity(new BigDecimal("10.00"));
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setTotalPrice(new BigDecimal("10000.00"));
        return item;
    }

    private ProjectEntity project(String projectId, CustomerProfileEntity customer) {
        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setCustomer(customer);
        project.setProjectCode("PRJ-2026-0001");
        project.setName("Bridge Construction");
        project.setLocation("Ho Chi Minh City");
        project.setStatus("ACTIVE");
        project.setCreatedAt(LocalDateTime.of(2026, 3, 1, 8, 0));
        return project;
    }

    private CustomerProfileEntity customer(String customerId, String companyName) {
        CustomerProfileEntity customer = new CustomerProfileEntity();
        customer.setId(customerId);
        customer.setCompanyName(companyName);
        customer.setTaxCode("0301234567");
        customer.setAddress("123 Nguyen Hue");
        customer.setContactPerson("Nguyen Van A");
        customer.setPhone("0909000000");
        customer.setEmail("customer-a@g90steel.vn");
        customer.setCustomerType("CONTRACTOR");
        customer.setStatus("ACTIVE");
        customer.setUser(user("customer-user", RoleName.CUSTOMER));
        return customer;
    }

    private UserAccountEntity user(String userId, RoleName roleName) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(userId);
        user.setEmail(userId + "@g90steel.vn");
        user.setFullName(roleName.name() + " User");
        user.setRole(role(roleName));
        return user;
    }

    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }

    private ProductEntity product(String productId, String productCode) {
        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setProductCode(productCode);
        product.setProductName("Steel Bar D16");
        product.setType("TON");
        product.setSize("D16");
        product.setThickness("16");
        product.setUnit("KG");
        product.setStatus(ProductStatus.ACTIVE.name());
        return product;
    }
}
