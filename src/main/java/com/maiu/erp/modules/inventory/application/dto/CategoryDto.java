package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

public class CategoryDto {
    private final UUID id;
    private final String name;
    private final UUID parentId;
    private final boolean active;

    public CategoryDto(
            UUID id,
            String name,
            UUID parentId,
            boolean active) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public boolean isActive() {
        return active;
    }
}
