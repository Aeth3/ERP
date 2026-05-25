package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;

public class PurchaseOrderDto {
    private final UUID id;
    private final String poNumber;
    private final UUID supplierId;
    private final UUID projectId;
    private final PurchaseOrderStatus status;
    private final LocalDate orderDate;
    private final LocalDate expectedDate;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private final List<PurchaseOrderItemDto> items;

    public PurchaseOrderDto(
            UUID id,
            String poNumber,
            UUID supplierId,
            UUID projectId,
            PurchaseOrderStatus status,
            LocalDate orderDate,
            LocalDate expectedDate,
            BigDecimal totalAmount,
            Instant createdAt,
            List<PurchaseOrderItemDto> items) {
        this.id = id;
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.projectId = projectId;
        this.status = status;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.items = items;
    }

    public UUID getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PurchaseOrderItemDto> getItems() {
        return items;
    }
}
