package com.maiu.erp.modules.project.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ProjectCostItemDto {
    private final UUID productId;
    private final String productName;
    private final BigDecimal issuedQuantity;
    private final BigDecimal issuedCost;

    public ProjectCostItemDto(
            UUID productId,
            String productName,
            BigDecimal issuedQuantity,
            BigDecimal issuedCost) {
        this.productId = productId;
        this.productName = productName;
        this.issuedQuantity = issuedQuantity;
        this.issuedCost = issuedCost;
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
}
