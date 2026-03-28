package com.g90.backend.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.DuplicateProductCodeException;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductImageEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.mapper.ProductMapper;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
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
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final ProductMapper productMapper = new ProductMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productRepository,
                auditLogRepository,
                productMapper,
                currentUserProvider,
                objectMapper,
                "WAREHOUSE_SOFT_DEACTIVATE"
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any())).thenAnswer(invocation -> {
            ProductEntity product = invocation.getArgument(0);
            if (product.getId() == null) {
                product.setId("product-1");
            }
            if (product.getCreatedAt() == null) {
                product.setCreatedAt(LocalDateTime.of(2026, 3, 28, 11, 0));
            }
            if (product.getUpdatedAt() == null) {
                product.setUpdatedAt(product.getCreatedAt());
            }
            int index = 1;
            for (ProductImageEntity image : product.getImages()) {
                if (image.getId() == null) {
                    image.setId("image-" + index++);
                }
                if (image.getCreatedAt() == null) {
                    image.setCreatedAt(LocalDateTime.of(2026, 3, 28, 11, 0));
                }
            }
            return product;
        });
    }

    @Test
    void createProductSuccess() {
        authenticateAs(RoleName.WAREHOUSE);
        when(productRepository.findByProductCodeIgnoreCase("SP001")).thenReturn(Optional.empty());

        var response = productService.createProduct(createRequest());

        assertThat(response.id()).isEqualTo("product-1");
        assertThat(response.productCode()).isEqualTo("SP001");
        assertThat(response.imageUrls()).containsExactly(
                "https://cdn.example.com/coil-1.jpg",
                "https://cdn.example.com/coil-2.jpg"
        );
        verify(productRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void createProductDuplicateCodeRejected() {
        authenticateAs(RoleName.WAREHOUSE);
        when(productRepository.findByProductCodeIgnoreCase("SP001")).thenReturn(Optional.of(existingProduct()));

        assertThatThrownBy(() -> productService.createProduct(createRequest()))
                .isInstanceOf(DuplicateProductCodeException.class);
    }

    @Test
    void updateProductSuccess() {
        authenticateAs(RoleName.WAREHOUSE);
        ProductEntity existing = existingProduct();
        when(productRepository.findDetailedById("product-1")).thenReturn(Optional.of(existing));
        when(productRepository.findByProductCodeIgnoreCase("SP002")).thenReturn(Optional.empty());

        var response = productService.updateProduct("product-1", updateRequest());

        assertThat(response.productCode()).isEqualTo("SP002");
        assertThat(response.productName()).isEqualTo("Steel Sheet Premium");
        assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE.name());
        assertThat(response.imageUrls()).containsExactly("https://cdn.example.com/sheet-premium.jpg");
        verify(auditLogRepository).save(any());
    }

    @Test
    void deleteProductFollowsSafePolicy() {
        authenticateAs(RoleName.WAREHOUSE);
        ProductEntity existing = existingProduct();
        when(productRepository.findDetailedById("product-1")).thenReturn(Optional.of(existing));

        var response = productService.deleteProduct("product-1");

        assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE.name());
        assertThat(response.deletedAt()).isNotNull();
        verify(productRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    private ProductCreateRequest createRequest() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode("SP001");
        request.setProductName("Steel Coil Prime");
        request.setType("COIL");
        request.setSize("1200 x 2400");
        request.setThickness("0.45");
        request.setUnit("KG");
        request.setWeightConversion(new BigDecimal("1.2500"));
        request.setReferenceWeight(new BigDecimal("1.2600"));
        request.setDescription("Primary warehouse coil");
        request.setImageUrls(List.of(
                "https://cdn.example.com/coil-1.jpg",
                "https://cdn.example.com/coil-2.jpg"
        ));
        return request;
    }

    private ProductUpdateRequest updateRequest() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductCode("SP002");
        request.setProductName("Steel Sheet Premium");
        request.setType("SHEET");
        request.setSize("1500 x 3000");
        request.setThickness("0.60");
        request.setUnit("KG");
        request.setWeightConversion(new BigDecimal("1.5000"));
        request.setReferenceWeight(new BigDecimal("1.5200"));
        request.setDescription("Updated catalog item");
        request.setStatus(ProductStatus.INACTIVE.name());
        request.setImageUrls(List.of("https://cdn.example.com/sheet-premium.jpg"));
        return request;
    }

    private ProductEntity existingProduct() {
        ProductEntity product = new ProductEntity();
        product.setId("product-1");
        product.setProductCode("SP001");
        product.setProductName("Steel Coil Prime");
        product.setType("COIL");
        product.setSize("1200 x 2400");
        product.setThickness("0.45");
        product.setUnit("KG");
        product.setWeightConversion(new BigDecimal("1.2500"));
        product.setReferenceWeight(new BigDecimal("1.2600"));
        product.setDescription("Primary warehouse coil");
        product.setStatus(ProductStatus.ACTIVE.name());
        product.setCreatedBy("warehouse-1");
        product.setUpdatedBy("warehouse-1");
        product.setCreatedAt(LocalDateTime.of(2026, 3, 27, 9, 0));
        product.setUpdatedAt(LocalDateTime.of(2026, 3, 27, 9, 0));
        product.setImages(new ArrayList<>(List.of(existingImage(product))));
        return product;
    }

    private ProductImageEntity existingImage(ProductEntity product) {
        ProductImageEntity image = new ProductImageEntity();
        image.setId("image-1");
        image.setProduct(product);
        image.setImageUrl("https://cdn.example.com/coil-1.jpg");
        image.setDisplayOrder(1);
        image.setCreatedAt(LocalDateTime.of(2026, 3, 27, 9, 0));
        return image;
    }

    private void authenticateAs(RoleName roleName) {
        when(currentUserProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser("warehouse-1", "warehouse@g90steel.vn", roleName.name(), "token")
        );
    }
}
