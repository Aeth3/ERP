package com.maiu.erp.modules.identity.application.dto;

import java.util.Set;

public class AuthDto {
    private final Long id;
    private final String token;
    private final String name;
    private final Set<String> roles;
    private final Set<String> permissions;

    public AuthDto(Long id, String token, String name, Set<String> roles, Set<String> permissions) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.roles = roles;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}
