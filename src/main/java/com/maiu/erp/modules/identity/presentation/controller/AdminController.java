package com.maiu.erp.modules.identity.presentation.controller;

import java.util.Map;
import java.util.Set;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.identity.application.dto.IdentityAuditEventDto;
import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.application.service.AdminService;
import com.maiu.erp.modules.identity.application.service.UserService;
import com.maiu.erp.modules.identity.domain.model.User;





@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody User user) {
        UserDto dto = userService.createUser(user);
        return ResponseEntity.status(201).body(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/uat/seed")
    public ResponseEntity<Map<String, String>> seedUatBaseline() {
        adminService.seedUatBaseline();
        return ResponseEntity.ok(Map.of(
                "message", "UAT baseline seeded successfully",
                "status", "OK"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit/identity")
    public ResponseEntity<List<IdentityAuditEventDto>> getIdentityAuditEvents() {
        return ResponseEntity.ok(adminService.getIdentityAuditEvents());
    }

}
