package com.g90.backend.exception;

import org.springframework.http.HttpStatus;

public class ContractCancellationSettlementNotFoundException extends ApiException {

    public ContractCancellationSettlementNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CONTRACT_CANCELLATION_SETTLEMENT_NOT_FOUND", "Contract cancellation settlement not found");
    }
}
