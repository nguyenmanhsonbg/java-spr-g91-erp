package com.g90.backend.modules.project.repository;

import com.g90.backend.modules.project.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, String> {
}
