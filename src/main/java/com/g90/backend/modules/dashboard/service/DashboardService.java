package com.g90.backend.modules.dashboard.service;

import com.g90.backend.modules.dashboard.dto.DashboardQuery;
import com.g90.backend.modules.dashboard.dto.DashboardResponseData;

public interface DashboardService {

    DashboardResponseData getDashboard(DashboardQuery query);
}
