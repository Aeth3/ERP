package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ProjectCostItemDto {
    private final UUID productId;
    private final String productName;
    private final BigDecimal issuedQuantity;
    private final BigDecimal issuedCost;
    private final BigDecimal returnedQuantity;
    private final BigDecimal returnedCost;
    private final BigDecimal netIssuedQuantity;
    private final BigDecimal netIssuedCost;

    public ProjectCostItemDto(
            UUID productId,
            String productName,
            BigDecimal issuedQuantity,
            BigDecimal issuedCost,
            BigDecimal returnedQuantity,
            BigDecimal returnedCost,
            BigDecimal netIssuedQuantity,
            BigDecimal netIssuedCost) {
        this.productId = productId;
        this.productName = productName;
        this.issuedQuantity = issuedQuantity;
        this.issuedCost = issuedCost;
        this.returnedQuantity = returnedQuantity;
        this.returnedCost = returnedCost;
        this.netIssuedQuantity = netIssuedQuantity;
        this.netIssuedCost = netIssuedCost;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getIssuedQuantity() {
        return issuedQuantity;
    }

    public BigDecimal getIssuedCost() {
        return issuedCost;
    }

    public BigDecimal getReturnedQuantity() {
        return returnedQuantity;
    }

    public BigDecimal getReturnedCost() {
        return returnedCost;
    }

    public BigDecimal getNetIssuedQuantity() {
        return netIssuedQuantity;
    }

    public BigDecimal getNetIssuedCost() {
        return netIssuedCost;
    }
}
