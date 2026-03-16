package com.g90.backend.modules.customer.repository;

import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerManagementRepository extends JpaRepository<CustomerProfileEntity, String>, JpaSpecificationExecutor<CustomerProfileEntity> {

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("select c from CustomerProfileEntity c where c.id = :id")
    Optional<CustomerProfileEntity> findDetailedById(@Param("id") String id);

    Optional<CustomerProfileEntity> findByTaxCodeIgnoreCase(String taxCode);

    @Query(
            value = """
                    select max(cast(substring(customer_code, 6) as unsigned))
                    from customers
                    where customer_code regexp '^CUST-[0-9]+$'
                    """,
            nativeQuery = true
    )
    Long findMaxCustomerCodeSequence();
}
