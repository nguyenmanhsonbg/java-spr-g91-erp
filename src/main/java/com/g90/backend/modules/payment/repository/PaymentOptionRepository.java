package com.g90.backend.modules.payment.repository;

import com.g90.backend.modules.payment.entity.PaymentOptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOptionRepository extends JpaRepository<PaymentOptionEntity, String> {

    List<PaymentOptionEntity> findByActiveTrueOrderByDisplayOrderAscCodeAsc();

    Optional<PaymentOptionEntity> findByCodeIgnoreCaseAndActiveTrue(String code);
}
