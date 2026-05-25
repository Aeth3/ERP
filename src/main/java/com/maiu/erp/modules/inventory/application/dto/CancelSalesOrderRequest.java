package com.maiu.erp.modules.inventory.application.dto;

import java.util.UUID;

public record CancelSalesOrderRequest(
        UUID warehouseId) {
}
