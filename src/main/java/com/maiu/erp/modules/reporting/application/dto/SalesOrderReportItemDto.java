package com.maiu.erp.modules.reporting.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SalesOrderReportItemDto {
    private final UUID id;
    private final String soNumber;
    private final String status;
    private final LocalDate orderDate;
    private final BigDecimal totalAmount;
    private final UUID customerId;
    private final String customerCode;
    private final String customerName;
    private final UUID confirmedWarehouseId;
    private final String confirmedWarehouseCode;
    private final String confirmedWarehouseName;
    private final int itemCount;
    private final int returnCount;

    public SalesOrderReportItemDto(
            UUID id,
            String soNumber,
            String status,
            LocalDate orderDate,
            BigDecimal totalAmount,
            UUID customerId,
            String customerCode,
            String customerName,
            UUID confirmedWarehouseId,
            String confirmedWarehouseCode,
            String confirmedWarehouseName,
            int itemCount,
            int returnCount) {
        this.id = id;
        this.soNumber = soNumber;
        this.status = status;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.confirmedWarehouseId = confirmedWarehouseId;
        this.confirmedWarehouseCode = confirmedWarehouseCode;
        this.confirmedWarehouseName = confirmedWarehouseName;
        this.itemCount = itemCount;
        this.returnCount = returnCount;
    }

    public UUID getId() {
        return id;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public UUID getConfirmedWarehouseId() {
        return confirmedWarehouseId;
    }

    public String getConfirmedWarehouseCode() {
        return confirmedWarehouseCode;
    }

    public String getConfirmedWarehouseName() {
        return confirmedWarehouseName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getReturnCount() {
        return returnCount;
    }
}
