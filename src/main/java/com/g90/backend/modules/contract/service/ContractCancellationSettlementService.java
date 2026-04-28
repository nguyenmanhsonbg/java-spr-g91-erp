package com.g90.backend.modules.contract.service;

import com.g90.backend.modules.contract.dto.ContractCancellationSettlementConfirmRequest;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementListResponseData;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementQuery;
import com.g90.backend.modules.contract.dto.ContractCancellationSettlementResponseData;

public interface ContractCancellationSettlementService {

    ContractCancellationSettlementListResponseData getSettlements(ContractCancellationSettlementQuery query);

    ContractCancellationSettlementResponseData getSettlement(String settlementId);

    ContractCancellationSettlementResponseData confirmRefund(
            String settlementId,
            ContractCancellationSettlementConfirmRequest request
    );
}
