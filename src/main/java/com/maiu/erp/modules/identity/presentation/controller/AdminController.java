package com.maiu.erp.modules.identity.presentation.controller;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.application.service.AdminService;





@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{email}/roles")
    public ResponseEntity<UserDto> assignRole(
            @PathVariable String email,
            @RequestBody Set<Long> roleIds) {
        System.out.println(email);
        System.out.println(roleIds);
        UserDto dto = adminService.assignRole(email, roleIds);

        return ResponseEntity.ok(dto);
    }

}