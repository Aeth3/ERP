package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

public class WarehouseDto {
    private final UUID id;
    private final String code;
    private final String name;
    private final String address;
    private final Boolean active;

    public WarehouseDto(
            UUID id,
            String code,
            String name,
            String address,
            Boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public Boolean getActive() {
        return active;
    }
}
