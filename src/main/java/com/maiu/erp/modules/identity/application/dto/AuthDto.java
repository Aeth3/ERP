package com.maiu.erp.modules.identity.application.dto;

import java.util.Set;

public class AuthDto {
    private final Long id;
    private final String token;
    private final String name;
    private final Set<String> roles;

    public AuthDto(Long id, String token, String name, Set<String> roles) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.roles = roles;
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
}
