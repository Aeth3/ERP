package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderRequest;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;

import jakarta.transaction.Transactional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository itemRepository,
            InventoryService inventoryService,
            ProductRepository productRepository,
            SupplierRepository supplierRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.itemRepository = itemRepository;
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
    }

    public List<PurchaseOrder> getPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getPurchaseOrderById(
            UUID purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("PO not found"));
    }

    public List<PurchaseOrderItem> getPurchaseOrderItems(
            UUID purchaseOrderId) {
        return itemRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    @Transactional
    public UUID createPurchaseOrder(
            CreatePurchaseOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new RuntimeException("Purchase order must have at least one item");
        }
        if (request.supplierId() == null) {
            throw new RuntimeException("Supplier ID is required");
        }
        if (request.orderDate() == null) {
            throw new RuntimeException("Order date is required");
        }
        supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNumber(generatePoNumber());
        purchaseOrder.setSupplierId(request.supplierId());
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        purchaseOrder.setOrderDate(request.orderDate());
        purchaseOrder.setExpectedDate(request.expectedDate());
        purchaseOrder.setCreatedAt(Instant.now());
        purchaseOrder.setTotalAmount(calculateTotalAmount(request.items()));

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        for (CreatePurchaseOrderItemRequest itemRequest : request.items()) {
            validatePurchaseOrderItem(itemRequest);

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrderId(savedPurchaseOrder.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.unitCost());
            item.setLineTotal(itemRequest.quantity().multiply(itemRequest.unitCost()));
            itemRepository.save(item);
        }

        return savedPurchaseOrder.getId();
    }

    @Transactional
    public void approvePurchaseOrder(
            UUID purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new RuntimeException("Only draft purchase orders can be approved");
        }

        if (itemRepository.findByPurchaseOrderId(purchaseOrderId).isEmpty()) {
            throw new RuntimeException("Purchase order has no items");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void receivePurchaseOrder(
            UUID purchaseOrderId,
            UUID warehouseId,
            UUID performedBy) {

        PurchaseOrder po = purchaseOrderRepository
                .findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new RuntimeException("PO must be approved");
        }

        List<PurchaseOrderItem> items =
                itemRepository.findByPurchaseOrderId(purchaseOrderId);

        for (PurchaseOrderItem item : items) {

            inventoryService.increaseStock(
                    item.getProductId(),
                    warehouseId,
                    item.getQuantity(),
                    item.getUnitCost(),
                    "PURCHASE_ORDER",
                    purchaseOrderId,
                    performedBy);
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);

        purchaseOrderRepository.save(po);
    }

    private BigDecimal calculateTotalAmount(
            List<CreatePurchaseOrderItemRequest> items) {
        return items.stream()
                .peek(this::validatePurchaseOrderItem)
                .map(item -> item.quantity().multiply(item.unitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validatePurchaseOrderItem(
            CreatePurchaseOrderItemRequest itemRequest) {
        if (itemRequest.productId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        if (itemRequest.quantity() == null
                || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Item quantity must be greater than zero");
        }
        if (itemRequest.unitCost() == null
                || itemRequest.unitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Item unit cost must be zero or greater");
        }

        productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    private String generatePoNumber() {
        String poNumber;
        do {
            poNumber = "PO-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (purchaseOrderRepository.findByPoNumber(poNumber).isPresent());

        return poNumber;
    }
}
