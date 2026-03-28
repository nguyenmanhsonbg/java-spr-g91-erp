package com.g90.backend.modules.inventory.service;

import com.g90.backend.modules.inventory.dto.InventoryAdjustmentRequest;
import com.g90.backend.modules.inventory.dto.InventoryHistoryQuery;
import com.g90.backend.modules.inventory.dto.InventoryHistoryResponseData;
import com.g90.backend.modules.inventory.dto.InventoryIssueRequest;
import com.g90.backend.modules.inventory.dto.InventoryMutationResponse;
import com.g90.backend.modules.inventory.dto.InventoryReceiptRequest;
import com.g90.backend.modules.inventory.dto.InventoryStatusQuery;
import com.g90.backend.modules.inventory.dto.InventoryStatusResponseData;

public interface InventoryService {

    InventoryMutationResponse createReceipt(InventoryReceiptRequest request);

    InventoryMutationResponse createIssue(InventoryIssueRequest request);

    InventoryMutationResponse adjustInventory(InventoryAdjustmentRequest request);

    InventoryStatusResponseData getInventoryStatus(InventoryStatusQuery query);

    InventoryHistoryResponseData getInventoryHistory(InventoryHistoryQuery query);
}
