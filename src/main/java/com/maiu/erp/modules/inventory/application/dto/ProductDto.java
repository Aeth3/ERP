package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProductDto {
    private final UUID id;
    private final String sku;
    private final String name;
    private final String description;
    private final BigDecimal costPrice;
    private final BigDecimal sellingPrice;
    private final Boolean active;
    private final UUID categoryId;
    private final UUID unitId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ProductDto(
            UUID id,
            String sku,
            String name,
            String description,
            BigDecimal costPrice,
            BigDecimal sellingPrice,
            Boolean active,
            UUID categoryId,
            UUID unitId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.active = active;
        this.categoryId = categoryId;
        this.unitId = unitId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public Boolean getActive() {
        return active;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
