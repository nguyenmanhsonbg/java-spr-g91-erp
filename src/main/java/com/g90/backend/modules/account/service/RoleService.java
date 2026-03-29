package com.g90.backend.modules.account.service;

import com.g90.backend.modules.account.dto.RoleResponse;
import java.util.List;

public interface RoleService {

    List<RoleResponse> getRoles();
}
