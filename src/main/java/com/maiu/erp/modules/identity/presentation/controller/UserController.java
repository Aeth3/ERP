package com.maiu.erp.modules.identity.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.application.service.AdminService;
import com.maiu.erp.modules.identity.application.service.UserService;

import java.util.List;

import com.maiu.erp.modules.identity.domain.model.User;



@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final AdminService adminService;

    public UserController(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody User user) {
        UserDto dto = userService.createUser(user);
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
    }

}
