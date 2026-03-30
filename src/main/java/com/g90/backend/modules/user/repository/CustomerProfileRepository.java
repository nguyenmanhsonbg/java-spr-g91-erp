package com.g90.backend.modules.user.repository;

import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, String> {

    Optional<CustomerProfileEntity> findByUser_Id(String userId);

    List<CustomerProfileEntity> findByUser_IdIn(Collection<String> userIds);

    @EntityGraph(attributePaths = {"user"})
    @Query("select c from CustomerProfileEntity c")
    List<CustomerProfileEntity> findAllWithUser();
}
