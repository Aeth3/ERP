package com.maiu.erp.modules.inventory.application.dto;

public class ActionResponse {
    private final String message;

    public ActionResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
