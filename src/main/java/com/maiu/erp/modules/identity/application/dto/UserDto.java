package com.maiu.erp.modules.identity.application.dto;

import java.util.Set;

public class UserDto {
    final private Long id;
    final private String username;
    final private String email;
    final private boolean emailVerified;
    final private Set<Long> roleIds;
    final private Set<String> roles;

    public UserDto(
            Long id,
            String username,
            String email,
            boolean emailVerified,
            Set<Long> roleIds,
            Set<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
        this.roleIds = roleIds;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
