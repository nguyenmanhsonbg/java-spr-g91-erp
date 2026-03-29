package com.g90.backend.modules.account.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.account.dto.RoleResponse;
import com.g90.backend.modules.account.service.RoleService;
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
@RequestMapping("/api/roles")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.success("Roles fetched successfully", roleService.getRoles());
    }
}
