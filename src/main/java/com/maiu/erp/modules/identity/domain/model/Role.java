package com.maiu.erp.modules.identity.domain.model;

import java.util.*;

public class Role {
    private Long id;
    private String name;
    private Set<String> permissions;

    public Role(Long id, String name, Set<String> permissions) {
        this.id = id;
        this.name = name;
        this.permissions = permissions;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getAuthority() {
        return "ROLE_" + name;
    }
}
