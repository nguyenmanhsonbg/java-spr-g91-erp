package com.g90.backend.modules.contract.service;

import com.g90.backend.exception.CustomerProfileNotFoundException;
import com.g90.backend.exception.ForbiddenOperationException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.contract.dto.ContractEventContractListQuery;
import com.g90.backend.modules.contract.dto.ContractEventContractListResponseData;
import com.g90.backend.modules.contract.entity.ContractStatus;
import com.g90.backend.modules.contract.entity.ContractTrackingEventEntity;
import com.g90.backend.modules.contract.entity.ContractTrackingEventType;
import com.g90.backend.modules.contract.repository.ContractTrackingEventRepository;
import com.g90.backend.modules.product.dto.PaginationResponse;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContractEventServiceImpl implements ContractEventService {

    private final ContractTrackingEventRepository contractTrackingEventRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public ContractEventContractListResponseData getContractsByEventStatus(ContractEventContractListQuery query) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        String scopedCustomerId = resolveCustomerScope(currentUser);
        normalizeQuery(query, scopedCustomerId == null);

        Page<ContractTrackingEventEntity> page = contractTrackingEventRepository.findLatestContractEventsByFilters(
                query.getEventStatus(),
                query.getEventType(),
                query.getKeyword(),
                query.getContractNumber(),
                scopedCustomerId != null ? scopedCustomerId : query.getCustomerId(),
                query.getEventFrom() == null ? null : query.getEventFrom().atStartOfDay(),
                query.getEventTo() == null ? null : query.getEventTo().plusDays(1).atStartOfDay().minusNanos(1),
                PageRequest.of(query.getPage() - 1, query.getPageSize())
        );

        String effectiveCustomerId = scopedCustomerId != null ? scopedCustomerId : query.getCustomerId();
        return new ContractEventContractListResponseData(
                page.getContent().stream().map(this::toItem).toList(),
                PaginationResponse.builder()
                        .page(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .totalItems(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build(),
                new ContractEventContractListResponseData.Filters(
                        query.getEventStatus(),
                        query.getEventType(),
                        query.getKeyword(),
                        query.getContractNumber(),
                        effectiveCustomerId,
                        query.getEventFrom(),
                        query.getEventTo()
                )
        );
    }

    private ContractEventContractListResponseData.Item toItem(ContractTrackingEventEntity event) {
        var contract = event.getContract();
        return new ContractEventContractListResponseData.Item(
                contract.getId(),
                contract.getContractNumber(),
                contract.getSaleOrderNumber(),
                contract.getCustomer() == null ? null : contract.getCustomer().getId(),
                contract.getCustomer() == null ? null : contract.getCustomer().getCompanyName(),
                contract.getStatus(),
                contract.getApprovalStatus(),
                contract.getTotalAmount(),
                contract.getExpectedDeliveryDate(),
                contract.getSubmittedAt(),
                event.getEventType(),
                event.getEventStatus(),
                event.getTitle(),
                event.getNote(),
                event.getActualAt() == null ? event.getCreatedAt() : event.getActualAt()
        );
    }

    private void normalizeQuery(ContractEventContractListQuery query, boolean internalUser) {
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setPageSize(query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize());
        query.setEventStatus(normalizeRequired(query.getEventStatus(), "eventStatus", "eventStatus is required"));
        query.setEventType(normalizeNullable(query.getEventType()));
        query.setKeyword(normalizeNullable(query.getKeyword()));
        query.setContractNumber(normalizeNullable(query.getContractNumber()));
        query.setCustomerId(internalUser ? normalizeNullable(query.getCustomerId()) : null);

        try {
            query.setEventStatus(ContractStatus.from(query.getEventStatus()).name());
        } catch (IllegalArgumentException exception) {
            throw RequestValidationException.singleError("eventStatus", "eventStatus is not valid");
        }

        if (StringUtils.hasText(query.getEventType())) {
            try {
                query.setEventType(ContractTrackingEventType.valueOf(query.getEventType().trim().toUpperCase(Locale.ROOT)).name());
            } catch (IllegalArgumentException exception) {
                throw RequestValidationException.singleError("eventType", "eventType is not valid");
            }
        }

        if (query.getEventFrom() != null && query.getEventTo() != null && query.getEventFrom().isAfter(query.getEventTo())) {
            throw RequestValidationException.singleError("eventFrom", "eventFrom must be on or before eventTo");
        }
    }

    private String resolveCustomerScope(AuthenticatedUser currentUser) {
        if (RoleName.CUSTOMER.name().equalsIgnoreCase(currentUser.role())) {
            CustomerProfileEntity customer = customerProfileRepository.findByUser_Id(currentUser.userId())
                    .orElseThrow(CustomerProfileNotFoundException::new);
            return customer.getId();
        }

        if (!RoleName.ACCOUNTANT.name().equalsIgnoreCase(currentUser.role())
                && !RoleName.OWNER.name().equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenOperationException("You do not have permission to perform this action");
        }
        return null;
    }

    private String normalizeRequired(String value, String field, String message) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw RequestValidationException.singleError(field, message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
