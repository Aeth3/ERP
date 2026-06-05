package com.maiu.erp.modules.identity.application.dto;

import java.util.Set;

public class UpdateRolePermissionsRequest {
    private Set<String> permissions;

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
