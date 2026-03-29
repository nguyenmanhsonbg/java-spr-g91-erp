package com.g90.backend.modules.project.service;

import com.g90.backend.modules.project.dto.WarehouseResponse;
import java.util.List;

public interface WarehouseService {

    List<WarehouseResponse> getWarehouses();
}
