package com.maiu.erp.modules.inventory.infrastructure.adapter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    public FinancePurchaseOrderReader(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            PurchaseReturnItemRepository purchaseReturnItemRepository,
            ProductRepository productRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.purchaseReturnItemRepository = purchaseReturnItemRepository;
        this.productRepository = productRepository;
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

        // Returns are recorded per product, not per PO item; spread each product's
        // returned quantity greedily across that product's items in PO order.
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrder.getId());
        List<PoLine> lines = new java.util.ArrayList<>();
        for (PurchaseOrderItem item : items) {
            BigDecimal remainingReturned = returnedByProduct.getOrDefault(item.getProductId(), BigDecimal.ZERO);
            BigDecimal allocated = remainingReturned.min(item.getQuantity());
            returnedByProduct.put(item.getProductId(), remainingReturned.subtract(allocated));
            String productName = productRepository.findById(item.getProductId())
                    .map(Product::getName)
                    .orElse(item.getProductId().toString());
            lines.add(new PoLine(
                    item.getId(),
                    item.getProductId(),
                    productName,
                    item.getQuantity(),
                    allocated,
                    item.getUnitCost()));
        }

        return new PoSnapshot(
                purchaseOrder.getId(),
                purchaseOrder.getPoNumber(),
                purchaseOrder.getSupplierId(),
                purchaseOrder.getStatus().name(),
                lines);
    }
}
