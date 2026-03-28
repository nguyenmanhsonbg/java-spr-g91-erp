package com.g90.backend.modules.pricing.service;

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
import com.g90.backend.exception.PriceListDeletionNotAllowedException;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.contract.repository.ContractRepository;
import com.g90.backend.modules.pricing.dto.PriceListCreateRequest;
import com.g90.backend.modules.pricing.dto.PriceListItemWriteRequest;
import com.g90.backend.modules.pricing.dto.PriceListUpdateRequest;
import com.g90.backend.modules.pricing.entity.PriceListEntity;
import com.g90.backend.modules.pricing.entity.PriceListItemEntity;
import com.g90.backend.modules.pricing.entity.PriceListStatus;
import com.g90.backend.modules.pricing.mapper.PricingMapper;
import com.g90.backend.modules.pricing.repository.PriceListRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingServiceImplTest {

    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final PricingMapper pricingMapper = new PricingMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private PricingServiceImpl pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl(
                priceListRepository,
                productRepository,
                contractRepository,
                auditLogRepository,
                pricingMapper,
                currentUserProvider,
                objectMapper
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(priceListRepository.save(any())).thenAnswer(invocation -> {
            PriceListEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("price-list-1");
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.of(2026, 3, 28, 10, 0));
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            int sequence = 1;
            for (PriceListItemEntity item : entity.getItems()) {
                if (item.getId() == null) {
                    item.setId("item-" + sequence++);
                }
                if (item.getCreatedAt() == null) {
                    item.setCreatedAt(LocalDateTime.of(2026, 3, 28, 10, 0));
                }
                if (item.getUpdatedAt() == null) {
                    item.setUpdatedAt(item.getCreatedAt());
                }
            }
            return entity;
        });
    }

    @Test
    void createPriceListSuccess() {
        authenticateAs(RoleName.OWNER);
        when(productRepository.findAllById(List.of("product-1"))).thenReturn(List.of(product("product-1", "SP001")));

        var response = pricingService.createPriceList(createRequest());

        assertThat(response.id()).isEqualTo("price-list-1");
        verify(priceListRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void createPriceListRejectsInvalidDateRange() {
        authenticateAs(RoleName.OWNER);
        PriceListCreateRequest request = createRequest();
        request.setValidFrom(LocalDate.of(2026, 5, 1));
        request.setValidTo(LocalDate.of(2026, 4, 30));

        assertThatThrownBy(() -> pricingService.createPriceList(request))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void updatePriceListSuccess() {
        authenticateAs(RoleName.OWNER);
        PriceListEntity existing = existingPriceList();
        when(priceListRepository.findDetailedById("price-list-1")).thenReturn(Optional.of(existing));
        when(productRepository.findAllById(List.of("product-1", "product-2")))
                .thenReturn(List.of(product("product-1", "SP001"), product("product-2", "SP002")));

        var response = pricingService.updatePriceList("price-list-1", updateRequest());

        assertThat(response.name()).isEqualTo("April Retail Pricing");
        assertThat(response.status()).isEqualTo(PriceListStatus.INACTIVE.name());
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).unitPriceVnd()).isEqualByComparingTo("125000.00");
        verify(auditLogRepository).save(any());
    }

    @Test
    void deletePriceListBlockedWhenUsedByActiveOrders() {
        authenticateAs(RoleName.OWNER);
        when(priceListRepository.findDetailedById("price-list-1")).thenReturn(Optional.of(existingPriceList()));
        when(contractRepository.existsByPriceListIdAndStatusIn(eq("price-list-1"), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> pricingService.deletePriceList("price-list-1"))
                .isInstanceOf(PriceListDeletionNotAllowedException.class);

        verify(priceListRepository, never()).save(any());
    }

    private PriceListCreateRequest createRequest() {
        PriceListCreateRequest request = new PriceListCreateRequest();
        request.setName("April Contractor Pricing");
        request.setCustomerGroup("CONTRACTOR");
        request.setValidFrom(LocalDate.of(2026, 4, 1));
        request.setValidTo(LocalDate.of(2026, 4, 30));
        request.setItems(List.of(itemRequest(null, "product-1", "120000.00", "Base price")));
        return request;
    }

    private PriceListUpdateRequest updateRequest() {
        PriceListUpdateRequest request = new PriceListUpdateRequest();
        request.setName("April Retail Pricing");
        request.setCustomerGroup("RETAIL");
        request.setValidFrom(LocalDate.of(2026, 4, 1));
        request.setValidTo(LocalDate.of(2026, 4, 30));
        request.setStatus(PriceListStatus.INACTIVE.name());
        request.setItems(List.of(
                itemRequest("item-1", "product-1", "125000.00", "Adjusted"),
                itemRequest(null, "product-2", "98000.00", "New product")
        ));
        return request;
    }

    private PriceListItemWriteRequest itemRequest(String id, String productId, String price, String note) {
        PriceListItemWriteRequest item = new PriceListItemWriteRequest();
        item.setId(id);
        item.setProductId(productId);
        item.setUnitPriceVnd(new BigDecimal(price));
        item.setNote(note);
        return item;
    }

    private PriceListEntity existingPriceList() {
        PriceListEntity entity = new PriceListEntity();
        entity.setId("price-list-1");
        entity.setName("Old Pricing");
        entity.setCustomerGroup("CONTRACTOR");
        entity.setValidFrom(LocalDate.of(2026, 4, 1));
        entity.setValidTo(LocalDate.of(2026, 4, 15));
        entity.setStatus(PriceListStatus.ACTIVE.name());
        entity.setCreatedBy("owner-1");
        entity.setUpdatedBy("owner-1");
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        entity.setItems(new ArrayList<>(List.of(existingItem(entity))));
        return entity;
    }

    private PriceListItemEntity existingItem(PriceListEntity priceList) {
        PriceListItemEntity item = new PriceListItemEntity();
        item.setId("item-1");
        item.setPriceList(priceList);
        item.setProduct(product("product-1", "SP001"));
        item.setUnitPrice(new BigDecimal("120000.00"));
        item.setPricingRuleType("FIXED_PRICE");
        item.setNote("Original");
        item.setCreatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 8, 0));
        return item;
    }

    private ProductEntity product(String id, String code) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setProductCode(code);
        product.setProductName("Steel Coil " + code);
        product.setType("STEEL");
        product.setSize("1200");
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

    @SuppressWarnings("unused")
    private RoleEntity role(RoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setId("role-" + roleName.name().toLowerCase());
        role.setName(roleName.name());
        return role;
    }
}
