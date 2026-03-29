package com.g90.backend.modules.project.service;

import com.g90.backend.modules.project.dto.WarehouseResponse;
import com.g90.backend.modules.project.repository.WarehouseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehouses() {
        return warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"))).stream()
                .map(warehouse -> new WarehouseResponse(
                        warehouse.getId(),
                        warehouse.getName(),
                        warehouse.getLocation()
                ))
                .toList();
    }
}
