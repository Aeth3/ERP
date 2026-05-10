package com.maiu.erp.modules.auth.presentation.controller;


import org.springframework.web.bind.annotation.*;

import com.maiu.erp.modules.auth.application.service.RoleService;

import java.util.List;

import com.maiu.erp.modules.auth.domain.model.Role;

@RestController
@RequestMapping("/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService){
        this.roleService = roleService;
    }
    @PostMapping
    public Role create(@RequestBody Role role){
        return roleService.createRole(role);
    }
    @GetMapping
    public List<Role>  getAll(){
        return roleService.getRoles();
    }

}
