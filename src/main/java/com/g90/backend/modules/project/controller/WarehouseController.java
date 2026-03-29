package com.g90.backend.modules.project.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.project.dto.WarehouseResponse;
import com.g90.backend.modules.project.service.WarehouseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouses")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ApiResponse<List<WarehouseResponse>> getWarehouses() {
        return ApiResponse.success("Warehouses fetched successfully", warehouseService.getWarehouses());
    }
}
