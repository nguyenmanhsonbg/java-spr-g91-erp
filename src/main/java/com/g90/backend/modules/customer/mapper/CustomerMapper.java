package com.g90.backend.modules.customer.mapper;

import com.g90.backend.modules.customer.dto.CustomerCreateResponse;
import com.g90.backend.modules.customer.dto.CustomerDetailResponseData;
import com.g90.backend.modules.customer.dto.CustomerListQuery;
import com.g90.backend.modules.customer.dto.CustomerListResponseData;
import com.g90.backend.modules.customer.dto.CustomerResponse;
import com.g90.backend.modules.customer.dto.CustomerStatusResponse;
import com.g90.backend.modules.customer.dto.CustomerSummaryResponseData;
import com.g90.backend.modules.customer.repository.CustomerAnalyticsRepository.CustomerAggregateSnapshot;
import com.g90.backend.modules.customer.repository.CustomerAnalyticsRepository.CustomerRecentTransactionSnapshot;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CustomerMapper {

    public CustomerCreateResponse toCreateResponse(CustomerProfileEntity customer, String temporaryPassword) {
        CustomerCreateResponse.PortalAccountData portalAccount = null;
        if (customer.getUser() != null) {
            portalAccount = new CustomerCreateResponse.PortalAccountData(
                    customer.getUser().getId(),
                    customer.getUser().getEmail(),
                    temporaryPassword
            );
        }

        return new CustomerCreateResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                customer.getStatus(),
                portalAccount
        );
    }

    public CustomerResponse toResponse(CustomerProfileEntity customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                customer.getTaxCode(),
                customer.getAddress(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getCustomerType(),
                customer.getPriceGroup(),
                scale(customer.getCreditLimit()),
                customer.getPaymentTerms(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public CustomerListResponseData toListResponse(Page<CustomerProfileEntity> page, CustomerListQuery query) {
        return new CustomerListResponseData(
                page.getContent().stream().map(this::toListItem).toList(),
                PaginationResponse.builder()
                        .page(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build(),
                new CustomerListResponseData.Filters(
                        query.getKeyword(),
                        query.getCustomerCode(),
                        query.getTaxCode(),
                        query.getCustomerType(),
                        query.getPriceGroup(),
                        query.getStatus(),
                        query.getCreatedFrom(),
                        query.getCreatedTo()
                )
        );
    }

    public CustomerDetailResponseData toDetailResponse(
            CustomerProfileEntity customer,
            CustomerAggregateSnapshot aggregateSnapshot,
            List<CustomerRecentTransactionSnapshot> recentTransactions
    ) {
        return new CustomerDetailResponseData(
                toResponse(customer),
                customer.getUser() == null
                        ? null
                        : new CustomerDetailResponseData.PortalAccountData(
                                customer.getUser().getId(),
                                customer.getUser().getEmail(),
                                customer.getUser().getStatus()
                        ),
                new CustomerDetailResponseData.FinancialData(
                        scale(customer.getCreditLimit()),
                        customer.getPaymentTerms(),
                        scale(aggregateSnapshot.totalInvoicedAmount()),
                        scale(aggregateSnapshot.totalPaymentsReceived()),
                        scale(aggregateSnapshot.totalAllocatedPayments()),
                        outstandingDebt(aggregateSnapshot)
                ),
                new CustomerDetailResponseData.ActivityData(
                        aggregateSnapshot.quotationCount(),
                        aggregateSnapshot.contractCount(),
                        aggregateSnapshot.invoiceCount(),
                        aggregateSnapshot.projectCount(),
                        aggregateSnapshot.activeProjectCount(),
                        aggregateSnapshot.openContractCount(),
                        aggregateSnapshot.lastTransactionAt()
                ),
                List.of(new CustomerDetailResponseData.ContactPersonData(
                        StringUtils.hasText(customer.getContactPerson()) ? customer.getContactPerson() : customer.getCompanyName(),
                        customer.getPhone(),
                        customer.getEmail(),
                        true
                )),
                recentTransactions.stream()
                        .map(item -> new CustomerDetailResponseData.RecentTransactionData(
                                item.type(),
                                item.entityId(),
                                item.referenceNo(),
                                item.status(),
                                scale(item.amount()),
                                item.eventAt()
                        ))
                        .toList(),
                List.of()
        );
    }

    public CustomerSummaryResponseData toSummaryResponse(
            CustomerProfileEntity customer,
            CustomerAggregateSnapshot aggregateSnapshot,
            List<String> disableBlockers
    ) {
        return new CustomerSummaryResponseData(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                customer.getStatus(),
                scale(customer.getCreditLimit()),
                customer.getPaymentTerms(),
                scale(aggregateSnapshot.totalInvoicedAmount()),
                scale(aggregateSnapshot.totalPaymentsReceived()),
                scale(aggregateSnapshot.totalAllocatedPayments()),
                outstandingDebt(aggregateSnapshot),
                aggregateSnapshot.quotationCount(),
                aggregateSnapshot.contractCount(),
                aggregateSnapshot.invoiceCount(),
                aggregateSnapshot.projectCount(),
                aggregateSnapshot.activeProjectCount(),
                aggregateSnapshot.openContractCount(),
                disableBlockers.isEmpty(),
                disableBlockers,
                aggregateSnapshot.lastTransactionAt()
        );
    }

    public CustomerStatusResponse toStatusResponse(CustomerProfileEntity customer, String reason) {
        return new CustomerStatusResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getStatus(),
                reason,
                customer.getUpdatedAt()
        );
    }

    private CustomerListResponseData.Item toListItem(CustomerProfileEntity customer) {
        return new CustomerListResponseData.Item(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                customer.getTaxCode(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getCustomerType(),
                customer.getPriceGroup(),
                scale(customer.getCreditLimit()),
                customer.getStatus(),
                customer.getUser() != null,
                customer.getCreatedAt()
        );
    }

    private BigDecimal outstandingDebt(CustomerAggregateSnapshot aggregateSnapshot) {
        BigDecimal debt = scale(aggregateSnapshot.totalInvoicedAmount())
                .subtract(scale(aggregateSnapshot.totalAllocatedPayments()));
        return debt.signum() < 0 ? BigDecimal.ZERO.setScale(2) : debt;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
