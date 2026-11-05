package com.maiu.erp.modules.finance.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ap_payment_allocations")
public class PaymentAllocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID billId;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBillId() { return billId; }
    public void setBillId(UUID billId) { this.billId = billId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
