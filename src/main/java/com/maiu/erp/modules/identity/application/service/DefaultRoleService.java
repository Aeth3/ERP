package com.maiu.erp.modules.identity.application.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.security.PermissionCatalog;
import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.repository.RoleRepository;

@Service
public class DefaultRoleService {

    private final RoleRepository roleRepository;

    public DefaultRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getOrCreateUserRole() {
        return getOrSyncRole("USER", PermissionCatalog.userPermissions());
    }

    public Role getOrCreateAdminRole() {
        return getOrSyncRole("ADMIN", PermissionCatalog.adminPermissions());
    }

    public Role getOrCreateViewerRole() {
        return getOrSyncRole("VIEWER", PermissionCatalog.viewerPermissions());
    }

    public Role getOrCreateProcurementRole() {
        return getOrSyncRole("PROCUREMENT", PermissionCatalog.procurementPermissions());
    }

    public Role getOrCreateWarehouseRole() {
        return getOrSyncRole("WAREHOUSE", PermissionCatalog.warehousePermissions());
    }

    public Role getOrCreateProjectManagerRole() {
        return getOrSyncRole("PROJECT_MANAGER", PermissionCatalog.projectManagerPermissions());
    }

    private Role getOrSyncRole(String roleName, Set<String> permissions) {
        Set<String> normalizedPermissions = new LinkedHashSet<>(PermissionCatalog.normalize(permissions));

        return roleRepository.findByName(roleName)
                .map(existing -> {
                    existing.setPermissions(normalizedPermissions);
                    return roleRepository.save(existing);
                })
                .orElseGet(() -> roleRepository.save(new Role(null, roleName, normalizedPermissions)));
    }
}
