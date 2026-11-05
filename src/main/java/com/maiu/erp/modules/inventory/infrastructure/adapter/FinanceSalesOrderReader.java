package com.maiu.erp.modules.inventory.infrastructure.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.maiu.erp.modules.finance.application.port.SalesOrderReader;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.SalesReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;

@Component
public class FinanceSalesOrderReader implements SalesOrderReader {
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final ProductRepository productRepository;

    public FinanceSalesOrderReader(
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            SalesReturnRepository salesReturnRepository,
            SalesReturnItemRepository salesReturnItemRepository,
            ProductRepository productRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.salesReturnItemRepository = salesReturnItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Optional<SoSnapshot> find(UUID salesOrderId) {
        return salesOrderRepository.findById(salesOrderId).map(this::toSnapshot);
    }

    private SoSnapshot toSnapshot(SalesOrder salesOrder) {
        Map<UUID, BigDecimal> returnedByProduct = new HashMap<>();
        for (SalesReturn salesReturn : salesReturnRepository.findBySalesOrderId(salesOrder.getId())) {
            for (SalesReturnItem item : salesReturnItemRepository.findBySalesReturnId(salesReturn.getId())) {
                returnedByProduct.merge(item.getProductId(), item.getQuantity(), BigDecimal::add);
            }
        }

        // Returns are recorded per product, not per SO item; spread each product's
        // returned quantity greedily across that product's items in SO order.
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());
        List<SoLine> lines = new ArrayList<>();
        for (SalesOrderItem item : items) {
            BigDecimal remainingReturned = returnedByProduct.getOrDefault(item.getProductId(), BigDecimal.ZERO);
            BigDecimal allocated = remainingReturned.min(item.getQuantity());
            returnedByProduct.put(item.getProductId(), remainingReturned.subtract(allocated));
            String productName = productRepository.findById(item.getProductId())
                    .map(Product::getName)
                    .orElse(item.getProductId().toString());
            lines.add(new SoLine(
                    item.getId(),
                    item.getProductId(),
                    productName,
                    item.getQuantity(),
                    allocated,
                    item.getUnitPrice()));
        }

        return new SoSnapshot(
                salesOrder.getId(),
                salesOrder.getSoNumber(),
                salesOrder.getCustomerId(),
                salesOrder.getStatus().name(),
                lines);
    }
}
