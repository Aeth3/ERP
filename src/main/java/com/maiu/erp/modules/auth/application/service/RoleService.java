package com.maiu.erp.modules.auth.application.service;


import org.springframework.stereotype.Service;

import com.maiu.erp.modules.auth.domain.repository.RoleRepository;

import java.util.List;

import com.maiu.erp.modules.auth.domain.model.Role;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
    }

    public Role createRole(Role role){

        return roleRepository.save(role);
    }
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }
}
