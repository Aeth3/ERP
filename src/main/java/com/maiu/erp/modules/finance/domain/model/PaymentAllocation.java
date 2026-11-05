package com.maiu.erp.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** How much of one payment settles one bill. */
public class PaymentAllocation {
    private UUID id;
    private UUID billId;
    private BigDecimal amount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBillId() { return billId; }
    public void setBillId(UUID billId) { this.billId = billId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
