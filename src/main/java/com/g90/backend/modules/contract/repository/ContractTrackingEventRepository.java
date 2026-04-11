package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractTrackingEventEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractTrackingEventRepository extends JpaRepository<ContractTrackingEventEntity, String> {

    List<ContractTrackingEventEntity> findByContract_IdOrderByActualAtAscCreatedAtAsc(String contractId);

    @EntityGraph(attributePaths = {"contract", "contract.customer"})
    @Query(
            value = """
                    select e
                    from ContractTrackingEventEntity e
                    join e.contract c
                    left join c.customer customer
                    where upper(e.eventStatus) = :eventStatus
                      and (:eventType is null or upper(e.eventType) = :eventType)
                      and (:keyword is null
                           or lower(c.contractNumber) like lower(concat('%', :keyword, '%'))
                           or lower(customer.companyName) like lower(concat('%', :keyword, '%'))
                           or lower(c.deliveryAddress) like lower(concat('%', :keyword, '%')))
                      and (:contractNumber is null or lower(c.contractNumber) like lower(concat('%', :contractNumber, '%')))
                      and (:customerId is null or customer.id = :customerId)
                      and (:eventFrom is null or coalesce(e.actualAt, e.createdAt) >= :eventFrom)
                      and (:eventTo is null or coalesce(e.actualAt, e.createdAt) <= :eventTo)
                      and coalesce(e.actualAt, e.createdAt) = (
                           select max(coalesce(e2.actualAt, e2.createdAt))
                           from ContractTrackingEventEntity e2
                           where e2.contract = c
                             and upper(e2.eventStatus) = :eventStatus
                             and (:eventType is null or upper(e2.eventType) = :eventType)
                             and (:eventFrom is null or coalesce(e2.actualAt, e2.createdAt) >= :eventFrom)
                             and (:eventTo is null or coalesce(e2.actualAt, e2.createdAt) <= :eventTo)
                      )
                    order by coalesce(e.actualAt, e.createdAt) desc, c.createdAt desc
                    """,
            countQuery = """
                    select count(distinct c.id)
                    from ContractTrackingEventEntity e
                    join e.contract c
                    left join c.customer customer
                    where upper(e.eventStatus) = :eventStatus
                      and (:eventType is null or upper(e.eventType) = :eventType)
                      and (:keyword is null
                           or lower(c.contractNumber) like lower(concat('%', :keyword, '%'))
                           or lower(customer.companyName) like lower(concat('%', :keyword, '%'))
                           or lower(c.deliveryAddress) like lower(concat('%', :keyword, '%')))
                      and (:contractNumber is null or lower(c.contractNumber) like lower(concat('%', :contractNumber, '%')))
                      and (:customerId is null or customer.id = :customerId)
                      and (:eventFrom is null or coalesce(e.actualAt, e.createdAt) >= :eventFrom)
                      and (:eventTo is null or coalesce(e.actualAt, e.createdAt) <= :eventTo)
                    """
    )
    Page<ContractTrackingEventEntity> findLatestContractEventsByFilters(
            @Param("eventStatus") String eventStatus,
            @Param("eventType") String eventType,
            @Param("keyword") String keyword,
            @Param("contractNumber") String contractNumber,
            @Param("customerId") String customerId,
            @Param("eventFrom") LocalDateTime eventFrom,
            @Param("eventTo") LocalDateTime eventTo,
            Pageable pageable
    );
}
