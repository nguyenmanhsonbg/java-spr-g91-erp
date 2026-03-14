package com.g90.backend.modules.contract.service;

import com.g90.backend.modules.contract.dto.ContractFromQuotationResponseData;
import com.g90.backend.modules.contract.dto.CreateContractFromQuotationRequest;

public interface ContractService {

    ContractFromQuotationResponseData createFromQuotation(String quotationId, CreateContractFromQuotationRequest request);
}
