package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.ProjectManagementEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectManagementRepository extends JpaRepository<ProjectManagementEntity, String>, JpaSpecificationExecutor<ProjectManagementEntity> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"customer", "customer.user", "primaryWarehouse", "backupWarehouse", "linkedContract", "milestones"})
    @Query("select p from ProjectManagementEntity p where p.id = :id")
    Optional<ProjectManagementEntity> findDetailedById(@Param("id") String id);

    @EntityGraph(attributePaths = {"customer", "customer.user", "primaryWarehouse", "backupWarehouse", "linkedContract", "milestones"})
    @Query("select p from ProjectManagementEntity p where p.id = :id and p.customer.id = :customerId")
    Optional<ProjectManagementEntity> findDetailedByIdAndCustomerId(@Param("id") String id, @Param("customerId") String customerId);
}
