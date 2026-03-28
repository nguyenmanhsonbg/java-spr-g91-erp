package com.g90.backend.modules.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.InsufficientInventoryException;
import com.g90.backend.exception.InvalidDateRangeException;
import com.g90.backend.exception.ProductNotFoundException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.inventory.dto.InventoryAdjustmentRequest;
import com.g90.backend.modules.inventory.dto.InventoryHistoryQuery;
import com.g90.backend.modules.inventory.dto.InventoryHistoryResponseData;
import com.g90.backend.modules.inventory.dto.InventoryIssueRequest;
import com.g90.backend.modules.inventory.dto.InventoryMutationResponse;
import com.g90.backend.modules.inventory.dto.InventoryPaginationResponse;
import com.g90.backend.modules.inventory.dto.InventoryReceiptRequest;
import com.g90.backend.modules.inventory.dto.InventoryStatusQuery;
import com.g90.backend.modules.inventory.dto.InventoryStatusResponseData;
import com.g90.backend.modules.inventory.entity.InventoryStockEntity;
import com.g90.backend.modules.inventory.entity.InventoryTransactionEntity;
import com.g90.backend.modules.inventory.entity.InventoryTransactionType;
import com.g90.backend.modules.inventory.mapper.InventoryMapper;
import com.g90.backend.modules.inventory.repository.InventoryProductSpecifications;
import com.g90.backend.modules.inventory.repository.InventoryStockRepository;
import com.g90.backend.modules.inventory.repository.InventoryTransactionRepository;
import com.g90.backend.modules.inventory.repository.InventoryTransactionSpecifications;
import com.g90.backend.modules.product.entity.ProductEntity;
import com.g90.backend.modules.product.repository.ProductRepository;
import com.g90.backend.modules.project.entity.WarehouseEntity;
import com.g90.backend.modules.project.repository.WarehouseRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_TYPE = "INVENTORY";
    private static final DateTimeFormatter CODE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> STATUS_VIEW_ROLES = Set.of(RoleName.WAREHOUSE.name(), RoleName.OWNER.name());

    private final ProductRepository productRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final InventoryMapper inventoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InventoryMutationResponse createReceipt(InventoryReceiptRequest request) {
        AuthenticatedUser currentUser = requireWarehouseOperator();
        validateReceiptRequest(request);

        ProductEntity product = loadProduct(request.getProductId());
        List<InventoryStockEntity> lockedStocks = inventoryStockRepository.lockByProductId(product.getId());
        BigDecimal quantity = normalizeQuantity(request.getQuantity());
        BigDecimal quantityBefore = totalQuantity(lockedStocks);

        InventoryStockEntity targetStock = resolveTargetStock(product, lockedStocks);
        targetStock.setQuantity(normalizeQuantity(targetStock.getQuantity()).add(quantity));
        targetStock.setUpdatedBy(currentUser.userId());
        inventoryStockRepository.save(targetStock);

        InventoryTransactionEntity transaction = buildTransaction(
                product,
                targetStock.getWarehouse(),
                InventoryTransactionType.RECEIPT,
                quantity,
                quantityBefore,
                quantityBefore.add(quantity),
                request.getReceiptDate(),
                request.getSupplierName(),
                null,
                null,
                request.getReason(),
                request.getNote(),
                currentUser.userId()
        );

        InventoryTransactionEntity saved = inventoryTransactionRepository.save(transaction);
        InventoryMutationResponse response = inventoryMapper.toMutationResponse(saved, currentUser.email());
        logAudit("CREATE_INVENTORY_RECEIPT", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public InventoryMutationResponse createIssue(InventoryIssueRequest request) {
        AuthenticatedUser currentUser = requireWarehouseOperator();
        validateIssueRequest(request);

        ProductEntity product = loadProduct(request.getProductId());
        List<InventoryStockEntity> lockedStocks = inventoryStockRepository.lockByProductId(product.getId());
        BigDecimal quantity = normalizeQuantity(request.getQuantity());
        BigDecimal quantityBefore = totalQuantity(lockedStocks);

        if (quantityBefore.compareTo(quantity) < 0) {
            throw new InsufficientInventoryException();
        }

        WarehouseEntity transactionWarehouse = consumeStocks(lockedStocks, quantity, currentUser.userId());
        BigDecimal quantityAfter = quantityBefore.subtract(quantity);
        InventoryTransactionEntity transaction = buildTransaction(
                product,
                transactionWarehouse,
                InventoryTransactionType.ISSUE,
                quantity,
                quantityBefore,
                quantityAfter,
                LocalDateTime.now(APP_ZONE),
                null,
                request.getRelatedOrderId(),
                request.getRelatedProjectId(),
                request.getReason(),
                request.getNote(),
                currentUser.userId()
        );

        InventoryTransactionEntity saved = inventoryTransactionRepository.save(transaction);
        InventoryMutationResponse response = inventoryMapper.toMutationResponse(saved, currentUser.email());
        logAudit("CREATE_INVENTORY_ISSUE", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional
    public InventoryMutationResponse adjustInventory(InventoryAdjustmentRequest request) {
        AuthenticatedUser currentUser = requireWarehouseOperator();
        validateAdjustmentRequest(request);

        ProductEntity product = loadProduct(request.getProductId());
        List<InventoryStockEntity> lockedStocks = inventoryStockRepository.lockByProductId(product.getId());
        BigDecimal adjustmentQuantity = normalizeQuantity(request.getAdjustmentQuantity());
        BigDecimal quantityBefore = totalQuantity(lockedStocks);
        BigDecimal quantityAfter = quantityBefore.add(adjustmentQuantity);

        if (quantityAfter.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) < 0) {
            throw RequestValidationException.singleError("adjustmentQuantity", "Resulting stock cannot be negative");
        }

        WarehouseEntity transactionWarehouse;
        if (adjustmentQuantity.signum() > 0) {
            InventoryStockEntity targetStock = resolveTargetStock(product, lockedStocks);
            targetStock.setQuantity(normalizeQuantity(targetStock.getQuantity()).add(adjustmentQuantity));
            targetStock.setUpdatedBy(currentUser.userId());
            inventoryStockRepository.save(targetStock);
            transactionWarehouse = targetStock.getWarehouse();
        } else {
            transactionWarehouse = consumeStocks(lockedStocks, adjustmentQuantity.abs(), currentUser.userId());
        }

        InventoryTransactionEntity transaction = buildTransaction(
                product,
                transactionWarehouse,
                InventoryTransactionType.ADJUSTMENT,
                adjustmentQuantity,
                quantityBefore,
                quantityAfter,
                LocalDateTime.now(APP_ZONE),
                null,
                null,
                null,
                request.getReason(),
                request.getNote(),
                currentUser.userId()
        );

        InventoryTransactionEntity saved = inventoryTransactionRepository.save(transaction);
        InventoryMutationResponse response = inventoryMapper.toMutationResponse(saved, currentUser.email());
        logAudit("CREATE_INVENTORY_ADJUSTMENT", saved.getId(), null, response, currentUser.userId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStatusResponseData getInventoryStatus(InventoryStatusQuery query) {
        requireInventoryViewer();
        normalizeStatusQuery(query);

        Page<ProductEntity> page = productRepository.findAll(
                InventoryProductSpecifications.withFilters(query),
                PageRequest.of(query.getPage() - 1, query.getSize(), Sort.by(Sort.Direction.ASC, "productName"))
        );

        List<String> productIds = page.getContent().stream().map(ProductEntity::getId).toList();
        Map<String, InventoryStockRepository.InventoryStockSummaryView> summariesByProductId = new LinkedHashMap<>();
        if (!productIds.isEmpty()) {
            inventoryStockRepository.summarizeByProductIds(productIds)
                    .forEach(summary -> summariesByProductId.put(summary.getProductId(), summary));
        }

        return InventoryStatusResponseData.builder()
                .items(page.getContent().stream()
                        .map(product -> {
                            InventoryStockRepository.InventoryStockSummaryView summary = summariesByProductId.get(product.getId());
                            BigDecimal quantity = summary == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalizeQuantity(summary.getQuantity());
                            LocalDateTime updatedAt = summary == null ? null : summary.getUpdatedAt();
                            return inventoryMapper.toStatusItem(product, quantity, updatedAt);
                        })
                        .toList())
                .pagination(InventoryPaginationResponse.builder()
                        .page(query.getPage())
                        .size(query.getSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .filters(InventoryStatusResponseData.Filters.builder()
                        .search(query.getSearch())
                        .productId(query.getProductId())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryHistoryResponseData getInventoryHistory(InventoryHistoryQuery query) {
        requireInventoryViewer();
        normalizeHistoryQuery(query);

        Page<InventoryTransactionEntity> page = inventoryTransactionRepository.findAll(
                InventoryTransactionSpecifications.withFilters(query),
                PageRequest.of(query.getPage() - 1, query.getSize(), Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "createdAt")))
        );

        Map<String, String> operatorEmails = loadOperatorEmails(page.getContent());
        return InventoryHistoryResponseData.builder()
                .items(page.getContent().stream()
                        .map(transaction -> inventoryMapper.toHistoryItem(transaction, operatorEmails))
                        .toList())
                .pagination(InventoryPaginationResponse.builder()
                        .page(query.getPage())
                        .size(query.getSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .filters(InventoryHistoryResponseData.Filters.builder()
                        .productId(query.getProductId())
                        .transactionType(query.getTransactionType())
                        .fromDate(query.getFromDate())
                        .toDate(query.getToDate())
                        .build())
                .build();
    }

    private void validateReceiptRequest(InventoryReceiptRequest request) {
        validatePositiveQuantity(request.getQuantity(), "quantity");
        validateReasonOrNote(request.getReason(), request.getNote());
    }

    private void validateIssueRequest(InventoryIssueRequest request) {
        validatePositiveQuantity(request.getQuantity(), "quantity");
        validateReasonOrNote(request.getReason(), request.getNote());
    }

    private void validateAdjustmentRequest(InventoryAdjustmentRequest request) {
        if (request.getAdjustmentQuantity() == null || request.getAdjustmentQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw RequestValidationException.singleError("adjustmentQuantity", "adjustmentQuantity must not be zero");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw RequestValidationException.singleError("reason", "reason is required");
        }
    }

    private void validatePositiveQuantity(BigDecimal quantity, String fieldName) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw RequestValidationException.singleError(fieldName, fieldName + " must be greater than 0");
        }
    }

    private void validateReasonOrNote(String reason, String note) {
        if (!StringUtils.hasText(reason) && !StringUtils.hasText(note)) {
            throw RequestValidationException.singleError("reason", "reason or note is required");
        }
    }

    private void normalizeStatusQuery(InventoryStatusQuery query) {
        query.setPage(normalizePage(query.getPage()));
        query.setSize(normalizeSize(query.getSize()));
        query.setSearch(normalizeNullable(query.getSearch()));
        query.setProductId(normalizeNullable(query.getProductId()));
    }

    private void normalizeHistoryQuery(InventoryHistoryQuery query) {
        query.setPage(normalizePage(query.getPage()));
        query.setSize(normalizeSize(query.getSize()));
        query.setProductId(normalizeNullable(query.getProductId()));
        query.setTransactionType(normalizeNullable(query.getTransactionType()));

        if (StringUtils.hasText(query.getTransactionType())) {
            try {
                query.setTransactionType(InventoryTransactionType.from(query.getTransactionType()).name());
            } catch (IllegalArgumentException exception) {
                throw RequestValidationException.singleError("transactionType", "transactionType must be RECEIPT, ISSUE, or ADJUSTMENT");
            }
        }
        if (query.getFromDate() != null && query.getToDate() != null && query.getFromDate().isAfter(query.getToDate())) {
            throw new InvalidDateRangeException();
        }
    }

    private InventoryTransactionEntity buildTransaction(
            ProductEntity product,
            WarehouseEntity warehouse,
            InventoryTransactionType type,
            BigDecimal quantity,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter,
            LocalDateTime transactionDate,
            String supplierName,
            String relatedOrderId,
            String relatedProjectId,
            String reason,
            String note,
            String createdBy
    ) {
        InventoryTransactionEntity transaction = new InventoryTransactionEntity();
        transaction.setTransactionCode(generateTransactionCode(type));
        transaction.setProduct(product);
        transaction.setWarehouse(warehouse);
        transaction.setTransactionType(type.name());
        transaction.setQuantity(normalizeQuantity(quantity));
        transaction.setQuantityBefore(normalizeQuantity(quantityBefore));
        transaction.setQuantityAfter(normalizeQuantity(quantityAfter));
        transaction.setTransactionDate(transactionDate);
        transaction.setSupplierName(normalizeNullable(supplierName));
        transaction.setRelatedOrderId(normalizeNullable(relatedOrderId));
        transaction.setRelatedProjectId(normalizeNullable(relatedProjectId));
        transaction.setReason(normalizeNullable(reason));
        transaction.setNote(normalizeNullable(note));
        transaction.setCreatedBy(createdBy);

        if (StringUtils.hasText(relatedOrderId)) {
            transaction.setReferenceType("ORDER");
            transaction.setReferenceId(relatedOrderId.trim());
        } else if (StringUtils.hasText(relatedProjectId)) {
            transaction.setReferenceType("PROJECT");
            transaction.setReferenceId(relatedProjectId.trim());
        }

        return transaction;
    }

    private ProductEntity loadProduct(String productId) {
        ProductEntity product = productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
        if (product.getDeletedAt() != null) {
            throw RequestValidationException.singleError("productId", "Product is archived");
        }
        return product;
    }

    private InventoryStockEntity resolveTargetStock(ProductEntity product, List<InventoryStockEntity> lockedStocks) {
        if (!lockedStocks.isEmpty()) {
            return lockedStocks.get(0);
        }

        InventoryStockEntity stock = new InventoryStockEntity();
        stock.setProduct(product);
        stock.setWarehouse(resolveDefaultWarehouse().orElse(null));
        stock.setQuantity(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return stock;
    }

    private WarehouseEntity consumeStocks(List<InventoryStockEntity> lockedStocks, BigDecimal requiredQuantity, String userId) {
        BigDecimal remaining = normalizeQuantity(requiredQuantity);
        WarehouseEntity transactionWarehouse = lockedStocks.isEmpty() ? resolveDefaultWarehouse().orElse(null) : lockedStocks.get(0).getWarehouse();

        for (InventoryStockEntity stock : lockedStocks) {
            if (remaining.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) == 0) {
                break;
            }

            BigDecimal currentQuantity = normalizeQuantity(stock.getQuantity());
            BigDecimal deduction = currentQuantity.min(remaining);
            if (deduction.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) > 0) {
                stock.setQuantity(currentQuantity.subtract(deduction));
                stock.setUpdatedBy(userId);
                inventoryStockRepository.save(stock);
                remaining = remaining.subtract(deduction);
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) > 0) {
            throw new InsufficientInventoryException();
        }
        return transactionWarehouse;
    }

    private BigDecimal totalQuantity(List<InventoryStockEntity> stocks) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (InventoryStockEntity stock : stocks) {
            total = total.add(normalizeQuantity(stock.getQuantity()));
        }
        return total;
    }

    private Map<String, String> loadOperatorEmails(List<InventoryTransactionEntity> transactions) {
        List<String> operatorIds = transactions.stream()
                .map(InventoryTransactionEntity::getCreatedBy)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, String> operatorEmails = new LinkedHashMap<>();
        if (operatorIds.isEmpty()) {
            return operatorEmails;
        }

        userAccountRepository.findByIdIn(operatorIds)
                .forEach(user -> operatorEmails.put(user.getId(), user.getEmail()));
        return operatorEmails;
    }

    private Optional<WarehouseEntity> resolveDefaultWarehouse() {
        return warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().findFirst();
    }

    private AuthenticatedUser requireWarehouseOperator() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!RoleName.WAREHOUSE.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("Only warehouse users can perform inventory transactions");
        }
        return currentUser;
    }

    private void requireInventoryViewer() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        if (!STATUS_VIEW_ROLES.contains(currentUser.role().toUpperCase(Locale.ROOT))) {
            throw new ForbiddenOperationException("You do not have permission to view inventory");
        }
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(ENTITY_TYPE);
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

    private String generateTransactionCode(InventoryTransactionType type) {
        String prefix = switch (type) {
            case RECEIPT -> "RC";
            case ISSUE -> "IS";
            case ADJUSTMENT -> "AD";
        };
        return "INV-" + prefix + "-" + LocalDateTime.now(APP_ZONE).format(CODE_TIMESTAMP) + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
