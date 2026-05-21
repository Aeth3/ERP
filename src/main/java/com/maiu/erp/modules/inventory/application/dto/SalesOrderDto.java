package com.maiu.erp.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;

public class SalesOrderDto {
    private final UUID id;
    private final String soNumber;
    private final UUID customerId;
    private final SalesOrderStatus status;
    private final LocalDate orderDate;
    private final BigDecimal totalAmount;
    private final List<SalesOrderItemDto> items;

    public SalesOrderDto(
            UUID id,
            String soNumber,
            UUID customerId,
            SalesOrderStatus status,
            LocalDate orderDate,
            BigDecimal totalAmount,
            List<SalesOrderItemDto> items) {
        this.id = id;
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.status = status;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public UUID getId() {
        return id;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public SalesOrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<SalesOrderItemDto> getItems() {
        return items;
    }
}
