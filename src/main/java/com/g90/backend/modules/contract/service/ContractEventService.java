package com.g90.backend.modules.contract.service;

import com.g90.backend.modules.contract.dto.ContractEventContractListQuery;
import com.g90.backend.modules.contract.dto.ContractEventContractListResponseData;

public interface ContractEventService {

    ContractEventContractListResponseData getContractsByEventStatus(ContractEventContractListQuery query);
}
