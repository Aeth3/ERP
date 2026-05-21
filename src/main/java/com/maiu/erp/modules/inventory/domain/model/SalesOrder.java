package com.maiu.erp.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;

public class SalesOrder {
    private UUID id;
    private String soNumber;
    private UUID customerId;
    private SalesOrderStatus status;
    private LocalDate orderDate;
    private BigDecimal totalAmount;

    public SalesOrderStatus getStatus() {
        return status;
    }

    public void setStatus(SalesOrderStatus status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setSoNumber(String soNumber) {
        this.soNumber = soNumber;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate =orderDate;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}