package com.maiu.erp.modules.identity.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.application.dto.IdentityAuditEventDto;
import com.maiu.erp.modules.identity.application.security.PermissionCatalog;
import com.maiu.erp.modules.identity.domain.repository.RoleRepository;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.shared.seed.UatSeedService;

import java.util.*;
import java.util.stream.Collectors;

import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;



@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UatSeedService uatSeedService;
    private final IdentityAuditService identityAuditService;
    private final boolean uatManualEnabled;
    private final String uatSharedPassword;

    public AdminService(UserRepository userRepository,
            RoleRepository roleRepository,
            UatSeedService uatSeedService,
            IdentityAuditService identityAuditService,
            @Value("${app.seed.uat.manual-enabled:false}") boolean uatManualEnabled,
            @Value("${app.seed.uat.shared-password:uat123}") String uatSharedPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.uatSeedService = uatSeedService;
        this.identityAuditService = identityAuditService;
        this.uatManualEnabled = uatManualEnabled;
        this.uatSharedPassword = uatSharedPassword;
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_USER_MANAGE + "')")
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.deleteUser(id);
        identityAuditService.recordUserDeleted(user);
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_ROLE_MANAGE + "')")
    public UserDto assignRole(String email, Set<Long> roleIds) {

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Set<String> beforeRoles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        if (roleIds == null || roleIds.isEmpty()) {
            throw new RuntimeException("Roles cannot be empty");
        }

        Set<Role> roles = roleIds.stream()
                .map(id -> roleRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + id)))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        User updatedUser = userRepository.save(user);
        identityAuditService.recordUserRolesUpdated(
                updatedUser.getEmail(),
                beforeRoles,
                updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));

        return new UserDto(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.isEmailVerified(),
                updatedUser.getRoles().stream().map(Role::getId).collect(Collectors.toSet()),
                updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.UAT_SEED_MANAGE + "')")
    public void seedUatBaseline() {
        if (!uatManualEnabled) {
            throw new RuntimeException("Manual UAT baseline seeding is disabled");
        }

        uatSeedService.seed(uatSharedPassword);
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_AUDIT_READ + "')")
    public List<IdentityAuditEventDto> getIdentityAuditEvents() {
        return identityAuditService.getRecentEvents();
    }
}
