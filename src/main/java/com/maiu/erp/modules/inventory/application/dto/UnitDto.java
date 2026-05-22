package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

public class UnitDto {
    private final UUID id;
    private final String name;
    private final String symbol;
    private final boolean active;

    public UnitDto(
            UUID id,
            String name,
            String symbol,
            boolean active) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isActive() {
        return active;
    }
}
