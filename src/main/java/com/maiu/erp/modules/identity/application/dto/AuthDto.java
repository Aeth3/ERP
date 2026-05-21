package com.maiu.erp.modules.identity.application.dto;

import java.util.Set;

public class AuthDto {
    private final String token;
    private final String name;
    private final Set<String> roles;

    public AuthDto(String token, String name, Set<String> roles) {
        this.token = token;
        this.name = name;
        this.roles = roles;
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
