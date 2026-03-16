package com.g90.backend.modules.customer.service;

import com.g90.backend.modules.customer.dto.CustomerCreateRequest;
import com.g90.backend.modules.customer.dto.CustomerCreateResponse;
import com.g90.backend.modules.customer.dto.CustomerDetailResponseData;
import com.g90.backend.modules.customer.dto.CustomerDisableRequest;
import com.g90.backend.modules.customer.dto.CustomerListQuery;
import com.g90.backend.modules.customer.dto.CustomerListResponseData;
import com.g90.backend.modules.customer.dto.CustomerStatusResponse;
import com.g90.backend.modules.customer.dto.CustomerSummaryResponseData;
import com.g90.backend.modules.customer.dto.CustomerUpdateRequest;

public interface CustomerService {

    CustomerCreateResponse createCustomer(CustomerCreateRequest request);

    CustomerListResponseData getCustomers(CustomerListQuery query);

    CustomerDetailResponseData getCustomerDetail(String id);

    CustomerDetailResponseData updateCustomer(String id, CustomerUpdateRequest request);

    CustomerStatusResponse disableCustomer(String id, CustomerDisableRequest request);

    CustomerSummaryResponseData getCustomerSummary(String id);
}
