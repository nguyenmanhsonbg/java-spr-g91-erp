package com.g90.backend.modules.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.InsufficientInventoryException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.inventory.dto.InventoryAdjustmentRequest;
import com.g90.backend.modules.inventory.dto.InventoryIssueRequest;
import com.g90.backend.modules.inventory.dto.InventoryReceiptRequest;
import com.g90.backend.modules.inventory.entity.InventoryStockEntity;
import com.g90.backend.modules.inventory.entity.InventoryTransactionEntity;
import com.g90.backend.modules.inventory.mapper.InventoryMapper;
import com.g90.backend.modules.inventory.repository.InventoryStockRepository;
import com.g90.backend.modules.inventory.repository.InventoryTransactionRepository;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.entity.ProductStatus;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.project.entity.WarehouseEntity;
import com.g90.backend.modules.project.repository.WarehouseRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryStockRepository inventoryStockRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final InventoryMapper inventoryMapper = new InventoryMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(
                productRepository,
                inventoryStockRepository,
                inventoryTransactionRepository,
                warehouseRepository,
                userAccountRepository,
                auditLogRepository,
                currentUserProvider,
                inventoryMapper,
                objectMapper
        );

        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryStockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.save(any())).thenAnswer(invocation -> {
            InventoryTransactionEntity transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId("tx-1");
            }
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(LocalDateTime.of(2026, 3, 28, 14, 0));
            }
            if (transaction.getTransactionDate() == null) {
                transaction.setTransactionDate(transaction.getCreatedAt());
            }
            return transaction;
        });
        when(warehouseRepository.findAll(any(Sort.class))).thenReturn(List.of(defaultWarehouse()));
        when(userAccountRepository.findByIdIn(anyCollection())).thenReturn(List.of());
    }

    @Test
    void createInventoryReceiptSuccess() {
        authenticateAs(RoleName.WAREHOUSE);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product()));
        when(inventoryStockRepository.lockByProductId("product-1")).thenReturn(List.of());

        var response = inventoryService.createReceipt(receiptRequest("100.00"));

        assertThat(response.transactionType()).isEqualTo("RECEIPT");
        assertThat(response.quantityBefore()).isEqualByComparingTo("0.00");
        assertThat(response.quantityAfter()).isEqualByComparingTo("100.00");
        verify(inventoryTransactionRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    void createInventoryReceiptWithInvalidQuantityRejected() {
        authenticateAs(RoleName.WAREHOUSE);

        assertThatThrownBy(() -> inventoryService.createReceipt(receiptRequest("0.00")))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void createInventoryIssueSuccess() {
        authenticateAs(RoleName.WAREHOUSE);
        InventoryStockEntity stock = stock("150.00");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product()));
        when(inventoryStockRepository.lockByProductId("product-1")).thenReturn(List.of(stock));

        var response = inventoryService.createIssue(issueRequest("50.00"));

        assertThat(response.transactionType()).isEqualTo("ISSUE");
        assertThat(response.quantityBefore()).isEqualByComparingTo("150.00");
        assertThat(response.quantityAfter()).isEqualByComparingTo("100.00");
        assertThat(stock.getQuantity()).isEqualByComparingTo("100.00");
    }

    @Test
    void createInventoryIssueRejectedWhenStockIsInsufficient() {
        authenticateAs(RoleName.WAREHOUSE);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product()));
        when(inventoryStockRepository.lockByProductId("product-1")).thenReturn(List.of(stock("40.00")));

        assertThatThrownBy(() -> inventoryService.createIssue(issueRequest("50.00")))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void adjustInventorySuccess() {
        authenticateAs(RoleName.WAREHOUSE);
        InventoryStockEntity stock = stock("100.00");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product()));
        when(inventoryStockRepository.lockByProductId("product-1")).thenReturn(List.of(stock));

        var response = inventoryService.adjustInventory(adjustmentRequest("-10.00", "Cycle count correction"));

        assertThat(response.transactionType()).isEqualTo("ADJUSTMENT");
        assertThat(response.quantity()).isEqualByComparingTo("-10.00");
        assertThat(response.quantityAfter()).isEqualByComparingTo("90.00");
        assertThat(stock.getQuantity()).isEqualByComparingTo("90.00");
    }

    @Test
    void adjustInventoryRejectedWhenReasonMissing() {
        authenticateAs(RoleName.WAREHOUSE);
        InventoryAdjustmentRequest request = adjustmentRequest("-10.00", " ");

        assertThatThrownBy(() -> inventoryService.adjustInventory(request))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void adjustInventoryRejectedWhenResultingStockIsNegative() {
        authenticateAs(RoleName.WAREHOUSE);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product()));
        when(inventoryStockRepository.lockByProductId("product-1")).thenReturn(List.of(stock("5.00")));

        assertThatThrownBy(() -> inventoryService.adjustInventory(adjustmentRequest("-10.00", "Count mismatch")))
                .isInstanceOf(RequestValidationException.class);
    }

    private InventoryReceiptRequest receiptRequest(String quantity) {
        InventoryReceiptRequest request = new InventoryReceiptRequest();
        request.setProductId("product-1");
        request.setQuantity(new BigDecimal(quantity));
        request.setReceiptDate(LocalDateTime.of(2026, 3, 28, 10, 0));
        request.setSupplierName("Steel Supplier");
        request.setReason("Supplier delivery");
        return request;
    }

    private InventoryIssueRequest issueRequest(String quantity) {
        InventoryIssueRequest request = new InventoryIssueRequest();
        request.setProductId("product-1");
        request.setQuantity(new BigDecimal(quantity));
        request.setRelatedProjectId("project-1");
        request.setReason("Project delivery");
        return request;
    }

    private InventoryAdjustmentRequest adjustmentRequest(String quantity, String reason) {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId("product-1");
        request.setAdjustmentQuantity(new BigDecimal(quantity));
        request.setReason(reason);
        request.setNote("Adjustment note");
        return request;
    }

    private ProductEntity product() {
        ProductEntity product = new ProductEntity();
        product.setId("product-1");
        product.setProductCode("SP001");
        product.setProductName("Steel Coil Prime");
        product.setType("COIL");
        product.setUnit("KG");
        product.setStatus(ProductStatus.ACTIVE.name());
        return product;
    }

    private InventoryStockEntity stock(String quantity) {
        InventoryStockEntity stock = new InventoryStockEntity();
        stock.setId("stock-1");
        stock.setProduct(product());
        stock.setWarehouse(defaultWarehouse());
        stock.setQuantity(new BigDecimal(quantity));
        stock.setUpdatedAt(LocalDateTime.of(2026, 3, 28, 9, 0));
        return stock;
    }

    private WarehouseEntity defaultWarehouse() {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setId("warehouse-1");
        warehouse.setName("Main Warehouse");
        return warehouse;
    }

    private void authenticateAs(RoleName roleName) {
        when(currentUserProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser("warehouse-1", "warehouse@g90steel.vn", roleName.name(), "token")
        );
    }
}
