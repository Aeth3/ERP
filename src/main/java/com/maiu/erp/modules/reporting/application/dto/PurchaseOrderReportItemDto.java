package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PurchaseOrderReportItemDto {
    private final UUID id;
    private final String poNumber;
    private final String status;
    private final LocalDate orderDate;
    private final LocalDate expectedDate;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private final UUID supplierId;
    private final String supplierCode;
    private final String supplierName;
    private final UUID projectId;
    private final String projectCode;
    private final String projectName;
    private final UUID receivedWarehouseId;
    private final String receivedWarehouseCode;
    private final String receivedWarehouseName;
    private final int itemCount;
    private final int returnCount;

    public PurchaseOrderReportItemDto(
            UUID id,
            String poNumber,
            String status,
            LocalDate orderDate,
            LocalDate expectedDate,
            BigDecimal totalAmount,
            Instant createdAt,
            UUID supplierId,
            String supplierCode,
            String supplierName,
            UUID projectId,
            String projectCode,
            String projectName,
            UUID receivedWarehouseId,
            String receivedWarehouseCode,
            String receivedWarehouseName,
            int itemCount,
            int returnCount) {
        this.id = id;
        this.poNumber = poNumber;
        this.status = status;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.receivedWarehouseId = receivedWarehouseId;
        this.receivedWarehouseCode = receivedWarehouseCode;
        this.receivedWarehouseName = receivedWarehouseName;
        this.itemCount = itemCount;
        this.returnCount = returnCount;
    }

    public UUID getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public String getStatus() {
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

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public UUID getReceivedWarehouseId() {
        return receivedWarehouseId;
    }

    public String getReceivedWarehouseCode() {
        return receivedWarehouseCode;
    }

    public String getReceivedWarehouseName() {
        return receivedWarehouseName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getReturnCount() {
        return returnCount;
    }
}
