package com.g90.backend.modules.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.InvalidDateRangeException;
import com.g90.backend.exception.PromotionDeletionNotAllowedException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.promotion.dto.PromotionCreateRequest;
import com.g90.backend.modules.promotion.dto.PromotionDetailResponse;
import com.g90.backend.modules.promotion.dto.PromotionUpdateRequest;
import com.g90.backend.modules.promotion.entity.PromotionCustomerGroupEntity;
import com.g90.backend.modules.promotion.entity.PromotionEntity;
import com.g90.backend.modules.promotion.entity.PromotionProductEntity;
import com.g90.backend.modules.promotion.entity.PromotionStatus;
import com.g90.backend.modules.promotion.entity.PromotionType;
import com.g90.backend.modules.promotion.mapper.PromotionMapper;
import com.g90.backend.modules.promotion.repository.PromotionRepository;
import com.g90.backend.modules.quotation.repository.QuotationRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final PromotionMapper promotionMapper = new PromotionMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private PromotionServiceImpl promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionServiceImpl(
                promotionRepository,
                productRepository,
                quotationRepository,
                customerProfileRepository,
                auditLogRepository,
                promotionMapper,
                currentUserProvider,
                objectMapper
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(promotionRepository.save(any())).thenAnswer(invocation -> {
            PromotionEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("promotion-1");
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.of(2026, 3, 28, 14, 0));
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            return entity;
        });
    }

    @Test
    void createPromotionSuccess() {
        authenticateAs(RoleName.OWNER);
        when(productRepository.findAllById(List.of("product-1"))).thenReturn(List.of(product("product-1", "SP001")));

        var response = promotionService.createPromotion(createRequest());

        assertThat(response.id()).isEqualTo("promotion-1");
        assertThat(response.code()).startsWith("PROMO-");
        verify(promotionRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void createPromotionInvalidDateRangeRejected() {
        authenticateAs(RoleName.OWNER);
        PromotionCreateRequest request = createRequest();
        request.setValidFrom(LocalDate.of(2026, 5, 1));
        request.setValidTo(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> promotionService.createPromotion(request))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void createPromotionInvalidDiscountValueRejected() {
        authenticateAs(RoleName.OWNER);
        PromotionCreateRequest request = createRequest();
        request.setPromotionType(PromotionType.PERCENT.name());
        request.setDiscountValue(new BigDecimal("150.00"));

        assertThatThrownBy(() -> promotionService.createPromotion(request))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void getPromotionDetailSuccess() {
        authenticateAs(RoleName.OWNER);
        when(promotionRepository.findDetailedById("promotion-1")).thenReturn(Optional.of(existingPromotion()));

        PromotionDetailResponse response = promotionService.getPromotionById("promotion-1");

        assertThat(response.id()).isEqualTo("promotion-1");
        assertThat(response.code()).isEqualTo("PROMO-APR-01");
        assertThat(response.products()).hasSize(1);
        assertThat(response.customerGroups()).containsExactly("DEFAULT");
    }

    @Test
    void updatePromotionSuccess() {
        authenticateAs(RoleName.OWNER);
        when(promotionRepository.findDetailedById("promotion-1")).thenReturn(Optional.of(existingPromotion()));
        when(productRepository.findAllById(List.of("product-1", "product-2")))
                .thenReturn(List.of(product("product-1", "SP001"), product("product-2", "SP002")));

        var response = promotionService.updatePromotion("promotion-1", updateRequest());

        assertThat(response.name()).isEqualTo("April Fixed Offer");
        assertThat(response.status()).isEqualTo(PromotionStatus.INACTIVE.name());
        assertThat(response.products()).hasSize(2);
        assertThat(response.customerGroups()).containsExactly("DEFAULT", "RETAIL");
        verify(auditLogRepository).save(any());
    }

    @Test
    void deletePromotionSuccessWhenNotInUse() {
        authenticateAs(RoleName.OWNER);
        when(promotionRepository.findDetailedById("promotion-1")).thenReturn(Optional.of(existingPromotion()));
        when(quotationRepository.existsByPromotionCodeIgnoreCaseAndStatusIn(eq("PROMO-APR-01"), anyCollection())).thenReturn(false);

        promotionService.deletePromotion("promotion-1");

        verify(promotionRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void deletePromotionBlockedWhenUsedByActiveOrders() {
        authenticateAs(RoleName.OWNER);
        when(promotionRepository.findDetailedById("promotion-1")).thenReturn(Optional.of(existingPromotion()));
        when(quotationRepository.existsByPromotionCodeIgnoreCaseAndStatusIn(eq("PROMO-APR-01"), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> promotionService.deletePromotion("promotion-1"))
                .isInstanceOf(PromotionDeletionNotAllowedException.class);

        verify(promotionRepository, never()).save(any());
    }

    private PromotionCreateRequest createRequest() {
        PromotionCreateRequest request = new PromotionCreateRequest();
        request.setName("April Promo");
        request.setPromotionType(PromotionType.PERCENT.name());
        request.setDiscountValue(new BigDecimal("5.00"));
        request.setValidFrom(LocalDate.of(2026, 4, 1));
        request.setValidTo(LocalDate.of(2026, 4, 30));
        request.setPriority(5);
        request.setDescription("Seasonal discount");
        request.setProductIds(List.of("product-1"));
        request.setCustomerGroups(List.of("DEFAULT"));
        return request;
    }

    private PromotionUpdateRequest updateRequest() {
        PromotionUpdateRequest request = new PromotionUpdateRequest();
        request.setName("April Fixed Offer");
        request.setPromotionType(PromotionType.FIXED_AMOUNT.name());
        request.setDiscountValue(new BigDecimal("100000.00"));
        request.setValidFrom(LocalDate.of(2026, 4, 1));
        request.setValidTo(LocalDate.of(2026, 4, 30));
        request.setStatus(PromotionStatus.INACTIVE.name());
        request.setPriority(9);
        request.setDescription("Updated promotion");
        request.setProductIds(List.of("product-1", "product-2"));
        request.setCustomerGroups(List.of("DEFAULT", "RETAIL"));
        return request;
    }

    private PromotionEntity existingPromotion() {
        PromotionEntity entity = new PromotionEntity();
        entity.setId("promotion-1");
        entity.setCode("PROMO-APR-01");
        entity.setName("Old Promo");
        entity.setPromotionType(PromotionType.PERCENT.name());
        entity.setDiscountValue(new BigDecimal("5.00"));
        entity.setStartDate(LocalDate.of(2026, 4, 1));
        entity.setEndDate(LocalDate.of(2026, 4, 15));
        entity.setStatus(PromotionStatus.ACTIVE.name());
        entity.setPriority(2);
        entity.setDescription("Old description");
        entity.setCreatedBy("owner-1");
        entity.setUpdatedBy("owner-1");
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        entity.setProducts(new ArrayList<>(List.of(existingProductScope(entity))));
        entity.setCustomerGroups(new ArrayList<>(List.of(existingCustomerGroupScope(entity))));
        return entity;
    }

    private PromotionProductEntity existingProductScope(PromotionEntity promotion) {
        PromotionProductEntity scope = new PromotionProductEntity();
        scope.setId("scope-product-1");
        scope.setPromotion(promotion);
        scope.setProduct(product("product-1", "SP001"));
        scope.setCreatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        return scope;
    }

    private PromotionCustomerGroupEntity existingCustomerGroupScope(PromotionEntity promotion) {
        PromotionCustomerGroupEntity scope = new PromotionCustomerGroupEntity();
        scope.setId("scope-group-1");
        scope.setPromotion(promotion);
        scope.setCustomerGroup("DEFAULT");
        scope.setCreatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        return scope;
    }

    private ProductEntity product(String id, String code) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setProductCode(code);
        product.setProductName("Steel Product " + code);
        product.setType("COIL");
        product.setSize("1200 x 2400");
        product.setThickness("0.45");
        product.setUnit("KG");
        product.setStatus(ProductStatus.ACTIVE.name());
        return product;
    }

    private void authenticateAs(RoleName roleName) {
        when(currentUserProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser("owner-1", "owner@g90steel.vn", roleName.name(), "token")
        );
    }
}
