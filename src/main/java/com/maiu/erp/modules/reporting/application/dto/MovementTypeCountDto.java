package com.maiu.erp.modules.reporting.application.dto;

public class MovementTypeCountDto {
    private final String movementType;
    private final int count;

    public MovementTypeCountDto(String movementType, int count) {
        this.movementType = movementType;
        this.count = count;
    }

    public String getMovementType() {
        return movementType;
    }

    public int getCount() {
        return count;
    }
}
