package com.g90.backend.modules.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardQuery {

    @Min(value = 1, message = "limit must be at least 1")
    @Max(value = 50, message = "limit must not exceed 50")
    private Integer limit = 10;
}
