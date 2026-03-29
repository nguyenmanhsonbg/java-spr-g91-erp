package com.g90.backend.modules.account.service;

import com.g90.backend.modules.account.dto.RoleResponse;
import com.g90.backend.modules.account.repository.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(role -> new RoleResponse(role.getId(), role.getName(), role.getDescription()))
                .toList();
    }
}
