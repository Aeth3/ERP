package com.maiu.erp.modules.inventory.infrastructure.adapter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;

@Component
public class FinancePurchaseOrderReader implements PurchaseOrderReader {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnItemRepository purchaseReturnItemRepository;

    public FinancePurchaseOrderReader(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            PurchaseReturnItemRepository purchaseReturnItemRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.purchaseReturnItemRepository = purchaseReturnItemRepository;
    }

    @Override
    public Optional<PoSnapshot> find(UUID purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId).map(this::toSnapshot);
    }

    private PoSnapshot toSnapshot(PurchaseOrder purchaseOrder) {
        Map<UUID, BigDecimal> returnedByProduct = new HashMap<>();
        for (PurchaseReturn purchaseReturn : purchaseReturnRepository.findByPurchaseOrderId(purchaseOrder.getId())) {
            for (PurchaseReturnItem item : purchaseReturnItemRepository.findByPurchaseReturnId(purchaseReturn.getId())) {
                returnedByProduct.merge(item.getProductId(), item.getQuantity(), BigDecimal::add);
            }
        }

        List<PoLine> lines = purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrder.getId()).stream()
                .map(item -> new PoLine(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        returnedByProduct.getOrDefault(item.getProductId(), BigDecimal.ZERO),
                        item.getUnitCost()))
                .toList();

        return new PoSnapshot(
                purchaseOrder.getId(),
                purchaseOrder.getPoNumber(),
                purchaseOrder.getSupplierId(),
                purchaseOrder.getStatus().name(),
                lines);
    }
}
