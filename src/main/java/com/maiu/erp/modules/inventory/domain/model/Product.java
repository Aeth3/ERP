package com.maiu.erp.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Product {
    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private Boolean active;
    private UUID categoryId;
    private UUID unitId;
    private Instant createdAt;
    private Instant updatedAt;

    public Product() {

    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public void setUnitId(UUID unitId) {
        this.unitId = unitId;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getUnitId() {
        return unitId;
    }
}
