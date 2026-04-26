package com.g90.backend.modules.dashboard.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.dashboard.dto.DashboardQuery;
import com.g90.backend.modules.dashboard.dto.DashboardResponseData;
import com.g90.backend.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponseData> getDashboard(@Valid @ModelAttribute DashboardQuery query) {
        return ApiResponse.success("Dashboard data fetched successfully", dashboardService.getDashboard(query));
    }
}
