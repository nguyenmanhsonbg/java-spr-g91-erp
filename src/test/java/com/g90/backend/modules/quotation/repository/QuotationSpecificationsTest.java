package com.g90.backend.modules.quotation.repository;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.quotation.dto.QuotationManagementListQuery;
import com.g90.backend.modules.quotation.entity.QuotationEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuotationSpecificationsTest {

    @Mock
    private Root<QuotationEntity> root;
    @Mock
    private CriteriaQuery<?> criteriaQuery;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private Predicate predicate;

    @Test
    void byQuerySkipsCustomerFilterWhenCustomerIdMissing() {
        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        QuotationManagementListQuery query = new QuotationManagementListQuery();

        QuotationSpecifications.byQuery(query, null).toPredicate(root, criteriaQuery, criteriaBuilder);

        verify(root, never()).get("customer");
        verify(criteriaBuilder, never()).equal(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull());
    }
}
