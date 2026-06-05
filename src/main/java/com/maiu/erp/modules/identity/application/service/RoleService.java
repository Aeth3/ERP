package com.maiu.erp.modules.identity.application.service;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.security.PermissionCatalog;
import com.maiu.erp.modules.identity.domain.repository.RoleRepository;

import java.util.List;
import java.util.Set;

import com.maiu.erp.modules.identity.domain.model.Role;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final IdentityAuditService identityAuditService;

    public RoleService(RoleRepository roleRepository, IdentityAuditService identityAuditService){
        this.roleRepository = roleRepository;
        this.identityAuditService = identityAuditService;
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_ROLE_MANAGE + "')")
    public Role createRole(Role role){
        Set<String> normalizedPermissions = PermissionCatalog.normalize(role.getPermissions());
        validatePermissions(normalizedPermissions);
        role.setPermissions(normalizedPermissions);
        Role savedRole = roleRepository.save(role);
        identityAuditService.recordRoleCreated(savedRole);
        return savedRole;
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_ROLE_READ + "')")
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_ROLE_MANAGE + "')")
    public Role updatePermissions(Long roleId, Set<String> permissions) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<String> normalizedPermissions = PermissionCatalog.normalize(permissions);
        validatePermissions(normalizedPermissions);
        Set<String> beforePermissions = role.getPermissions() == null ? Set.of() : role.getPermissions();
        role.setPermissions(normalizedPermissions);

        Role updatedRole = roleRepository.save(role);
        identityAuditService.recordRolePermissionsUpdated(updatedRole, beforePermissions, normalizedPermissions);
        return updatedRole;
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.IDENTITY_ROLE_READ + "')")
    public Set<String> getPermissionCatalog() {
        return PermissionCatalog.allPermissions();
    }

    private void validatePermissions(Set<String> permissions) {
        Set<String> catalog = PermissionCatalog.allPermissions();
        permissions.forEach(permission -> {
            if (!catalog.contains(permission)) {
                throw new RuntimeException("Unknown permission: " + permission);
            }
        });
    }
}
