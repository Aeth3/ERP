package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

public class CustomerDto {
    private final UUID id;
    private final String code;
    private final String name;
    private final String contactPerson;
    private final String phone;
    private final String email;
    private final String address;
    private final Boolean active;

    public CustomerDto(
            UUID id,
            String code,
            String name,
            String contactPerson,
            String phone,
            String email,
            String address,
            Boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public Boolean getActive() {
        return active;
    }
}
