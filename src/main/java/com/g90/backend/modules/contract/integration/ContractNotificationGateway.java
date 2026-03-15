package com.g90.backend.modules.contract.integration;

import com.g90.backend.modules.contract.entity.ContractApprovalType;
import com.g90.backend.modules.contract.entity.ContractEntity;
import org.springframework.stereotype.Component;

public interface ContractNotificationGateway {

    void notifyWarehousePreparation(ContractEntity contract, String message);

    void notifyApprovalRequested(ContractEntity contract, ContractApprovalType approvalType, String message);

    void notifyApprovalDecision(ContractEntity contract, String decision, String message);

    void notifyCancellation(ContractEntity contract, String message);
}

@Component
class NoopContractNotificationGateway implements ContractNotificationGateway {

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
