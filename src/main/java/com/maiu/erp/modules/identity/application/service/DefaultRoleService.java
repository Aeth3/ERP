package com.maiu.erp.modules.identity.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.repository.RoleRepository;

@Service
public class DefaultRoleService {

    private final RoleRepository roleRepository;

    public DefaultRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getOrCreateUserRole() {
        return roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER", Set.of())));
    }

    public Role getOrCreateAdminRole() {
        return roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN", Set.of())));
    }
}
