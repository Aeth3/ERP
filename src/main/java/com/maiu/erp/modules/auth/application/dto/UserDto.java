package com.maiu.erp.modules.auth.application.dto;

public class UserDto {
    final private Long id;
    final private String username;
    final private String email;

    public UserDto(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
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
}
