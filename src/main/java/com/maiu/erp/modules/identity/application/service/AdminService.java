package com.maiu.erp.modules.identity.application.service;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.domain.repository.RoleRepository;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;



@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminService(UserRepository userRepository,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        userRepository.deleteUser(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserDto assignRole(String email, Set<Long> roleIds) {

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (roleIds == null || roleIds.isEmpty()) {
            throw new RuntimeException("Roles cannot be empty");
        }

        Set<Role> roles = roleIds.stream()
                .map(id -> roleRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + id)))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        User updatedUser = userRepository.save(user);

        return new UserDto(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.isEmailVerified());
    }
}
