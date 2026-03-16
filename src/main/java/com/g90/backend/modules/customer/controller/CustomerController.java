package com.g90.backend.modules.customer.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.customer.dto.CustomerCreateRequest;
import com.g90.backend.modules.customer.dto.CustomerCreateResponse;
import com.g90.backend.modules.customer.dto.CustomerDetailResponseData;
import com.g90.backend.modules.customer.dto.CustomerDisableRequest;
import com.g90.backend.modules.customer.dto.CustomerListQuery;
import com.g90.backend.modules.customer.dto.CustomerListResponseData;
import com.g90.backend.modules.customer.dto.CustomerStatusResponse;
import com.g90.backend.modules.customer.dto.CustomerSummaryResponseData;
import com.g90.backend.modules.customer.dto.CustomerUpdateRequest;
import com.g90.backend.modules.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerCreateResponse>> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", customerService.createCustomer(request)));
    }

    @GetMapping
    public ApiResponse<CustomerListResponseData> getCustomers(@Valid @ModelAttribute CustomerListQuery query) {
        return ApiResponse.success("Customer list fetched successfully", customerService.getCustomers(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailResponseData> getCustomerDetail(@PathVariable String id) {
        return ApiResponse.success("Customer detail fetched successfully", customerService.getCustomerDetail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerDetailResponseData> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        return ApiResponse.success("Customer updated successfully", customerService.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<CustomerStatusResponse> disableCustomer(
            @PathVariable String id,
            @Valid @RequestBody CustomerDisableRequest request
    ) {
        return ApiResponse.success("Customer disabled successfully", customerService.disableCustomer(id, request));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<CustomerSummaryResponseData> getCustomerSummary(@PathVariable String id) {
        return ApiResponse.success("Customer summary fetched successfully", customerService.getCustomerSummary(id));
    }
}
