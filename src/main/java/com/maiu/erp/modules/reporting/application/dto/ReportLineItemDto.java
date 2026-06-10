package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ReportLineItemDto {
    private final UUID productId;
    private final String productSku;
    private final String productName;
    private final BigDecimal quantity;
    private final BigDecimal unitAmount;
    private final BigDecimal lineTotal;

    public ReportLineItemDto(
            UUID productId,
            String productSku,
            String productName,
            BigDecimal quantity,
            BigDecimal unitAmount,
            BigDecimal lineTotal) {
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
        this.lineTotal = lineTotal;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
