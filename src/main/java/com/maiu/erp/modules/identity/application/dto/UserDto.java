package com.maiu.erp.modules.identity.application.dto;

public class UserDto {
    final private Long id;
    final private String username;
    final private String email;
    final private boolean emailVerified;

    public UserDto(Long id, String username, String email, boolean emailVerified) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
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
}
