package com.g90.backend.modules.contract.repository;

import com.g90.backend.modules.contract.entity.ContractCancellationSettlementEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractCancellationSettlementRepository extends JpaRepository<ContractCancellationSettlementEntity, String> {

    List<ContractCancellationSettlementEntity> findByContract_IdOrderByCreatedAtDesc(String contractId);

    @EntityGraph(attributePaths = {"contract", "customer", "customer.user"})
    @Query("select s from ContractCancellationSettlementEntity s order by s.createdAt desc")
    List<ContractCancellationSettlementEntity> findAllDetailed();

    @EntityGraph(attributePaths = {"contract", "customer", "customer.user"})
    @Query("select s from ContractCancellationSettlementEntity s where s.customer.id = :customerId order by s.createdAt desc")
    List<ContractCancellationSettlementEntity> findByCustomerIdDetailed(@Param("customerId") String customerId);

    @EntityGraph(attributePaths = {"contract", "customer", "customer.user"})
    @Query("select s from ContractCancellationSettlementEntity s where s.id = :id")
    Optional<ContractCancellationSettlementEntity> findDetailedById(@Param("id") String id);
}
