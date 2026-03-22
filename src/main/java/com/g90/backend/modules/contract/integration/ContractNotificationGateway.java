package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.contract.entity.ContractApprovalType;
import com.g90.backend.modules.contract.entity.ContractEntity;

public interface ContractNotificationGateway {

    void notifyContractCreated(ContractEntity contract, String message);

    void notifyContractApproved(ContractEntity contract, String message);

    void notifyWarehousePreparation(ContractEntity contract, String message);

    void notifyApprovalRequested(ContractEntity contract, ContractApprovalType approvalType, String message);

    void notifyApprovalDecision(ContractEntity contract, String decision, String message);

    void notifyCancellation(ContractEntity contract, String message);
}

class NoopContractNotificationGateway implements ContractNotificationGateway {

    @Override
    public void notifyContractCreated(ContractEntity contract, String message) {
    }

    @Override
    public void notifyContractApproved(ContractEntity contract, String message) {
    }

    @Override
    public void notifyWarehousePreparation(ContractEntity contract, String message) {
    }

    @Override
    public void notifyApprovalRequested(ContractEntity contract, ContractApprovalType approvalType, String message) {
    }

    @Override
    public void notifyApprovalDecision(ContractEntity contract, String decision, String message) {
    }

    @Override
    public void notifyCancellation(ContractEntity contract, String message) {
    }
}
