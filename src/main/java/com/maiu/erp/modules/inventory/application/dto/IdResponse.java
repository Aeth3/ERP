package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

public class IdResponse {
    private final UUID id;

    public IdResponse(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
