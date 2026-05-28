package com.maiu.erp.modules.inventory.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseOrderRequest;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnItemRequest;
import com.maiu.erp.modules.inventory.application.dto.CreatePurchaseReturnRequest;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnItemRepository purchaseReturnItemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Autowired
    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository itemRepository,
            InventoryService inventoryService,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            ProjectRepository projectRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            PurchaseReturnItemRepository purchaseReturnItemRepository,
            StockMovementRepository stockMovementRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.itemRepository = itemRepository;
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.projectRepository = projectRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.purchaseReturnItemRepository = purchaseReturnItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository itemRepository,
            InventoryService inventoryService,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            ProjectRepository projectRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            PurchaseReturnItemRepository purchaseReturnItemRepository) {
        this(
                purchaseOrderRepository,
                itemRepository,
                inventoryService,
                productRepository,
                supplierRepository,
                projectRepository,
                purchaseReturnRepository,
                purchaseReturnItemRepository,
                null);
    }

    public List<PurchaseOrder> getPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getPurchaseOrderById(
            UUID purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("PO not found"));
    }

    public List<PurchaseOrderItem> getPurchaseOrderItems(
            UUID purchaseOrderId) {
        return itemRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    @Transactional
    public UUID createPurchaseOrder(
            CreatePurchaseOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Purchase order must have at least one item");
        }
        if (request.supplierId() == null) {
            throw new BadRequestException("Supplier ID is required");
        }
        if (request.orderDate() == null) {
            throw new BadRequestException("Order date is required");
        }
        supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
        validateProjectId(request.projectId());

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNumber(generatePoNumber());
        purchaseOrder.setSupplierId(request.supplierId());
        purchaseOrder.setProjectId(request.projectId());
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
                .orElseThrow(() -> new NotFoundException("PO not found"));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BadRequestException("Only draft purchase orders can be approved");
        }

        if (itemRepository.findByPurchaseOrderId(purchaseOrderId).isEmpty()) {
            throw new BadRequestException("Purchase order has no items");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void receivePurchaseOrder(
            UUID purchaseOrderId,
            UUID warehouseId,
            String performedBy) {
        inventoryService.validateWarehouseAndActor(warehouseId, performedBy);

        PurchaseOrder po = purchaseOrderRepository
                .findById(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("PO not found"));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new BadRequestException("PO must be approved");
        }

        List<PurchaseOrderItem> items =
                itemRepository.findByPurchaseOrderId(purchaseOrderId);

        if (items.isEmpty()) {
            throw new BadRequestException("Purchase order has no items");
        }

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

        po.setReceivedWarehouseId(warehouseId);
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public UUID createPurchaseReturn(UUID purchaseOrderId, CreatePurchaseReturnRequest request) {
        PurchaseOrder purchaseOrder = getPurchaseOrderById(purchaseOrderId);
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.RECEIVED) {
            throw new BadRequestException("Purchase order must be received");
        }
        UUID receivedWarehouseId = resolveReceivedWarehouseId(purchaseOrder);
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Purchase return must have at least one item");
        }

        String performedBy = normalizePerformedBy(request.performedBy());
        List<PurchaseOrderItem> purchaseOrderItems = itemRepository.findByPurchaseOrderId(purchaseOrderId);
        if (purchaseOrderItems.isEmpty()) {
            throw new BadRequestException("Purchase order has no items");
        }

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setReturnNumber(generatePurchaseReturnNumber());
        purchaseReturn.setPurchaseOrderId(purchaseOrderId);
        purchaseReturn.setSupplierId(purchaseOrder.getSupplierId());
        purchaseReturn.setWarehouseId(receivedWarehouseId);
        purchaseReturn.setRemarks(trimToNull(request.remarks()));
        purchaseReturn.setPerformedBy(performedBy);
        purchaseReturn.setReturnedAt(Instant.now());
        PurchaseReturn savedReturn = purchaseReturnRepository.save(purchaseReturn);

        for (CreatePurchaseReturnItemRequest itemRequest : request.items()) {
            validatePurchaseReturnItem(itemRequest, purchaseOrderItems, purchaseOrderId);

            PurchaseOrderItem sourceItem = purchaseOrderItems.stream()
                    .filter(item -> itemRequest.productId().equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Purchase return item product was not on the purchase order"));

            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setPurchaseReturnId(savedReturn.getId());
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(sourceItem.getUnitCost());
            item.setLineTotal(itemRequest.quantity().multiply(sourceItem.getUnitCost()));
            purchaseReturnItemRepository.save(item);

            inventoryService.returnPurchaseStock(
                    item.getProductId(),
                    savedReturn.getWarehouseId(),
                    item.getQuantity(),
                    item.getUnitCost(),
                    savedReturn.getId(),
                    savedReturn.getRemarks(),
                    performedBy);
        }

        return savedReturn.getId();
    }

    public List<PurchaseReturn> getPurchaseReturns() {
        return purchaseReturnRepository.findAll();
    }

    public PurchaseReturn getPurchaseReturnById(UUID returnId) {
        return purchaseReturnRepository.findById(returnId)
                .orElseThrow(() -> new NotFoundException("Purchase return not found"));
    }

    public List<PurchaseReturnItem> getPurchaseReturnItems(UUID returnId) {
        return purchaseReturnItemRepository.findByPurchaseReturnId(returnId);
    }

    @Transactional
    public void cancelPurchaseOrder(
            UUID purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("PO not found"));

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BadRequestException("Received purchase orders cannot be cancelled");
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BadRequestException("Purchase order is already cancelled");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(purchaseOrder);
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
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null
                || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Item quantity must be greater than zero");
        }
        if (itemRequest.unitCost() == null
                || itemRequest.unitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Item unit cost must be zero or greater");
        }

        productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void validateProjectId(UUID projectId) {
        if (projectId == null) {
            return;
        }

        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private void validatePurchaseReturnItem(
            CreatePurchaseReturnItemRequest itemRequest,
            List<PurchaseOrderItem> purchaseOrderItems,
            UUID purchaseOrderId) {
        if (itemRequest.productId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (itemRequest.quantity() == null || itemRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Return quantity must be greater than zero");
        }

        PurchaseOrderItem sourceItem = purchaseOrderItems.stream()
                .filter(item -> itemRequest.productId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Purchase return item product was not on the purchase order"));

        BigDecimal alreadyReturnedQuantity = getReturnedQuantityForProduct(purchaseOrderId, itemRequest.productId());
        BigDecimal newReturnedQuantity = alreadyReturnedQuantity.add(itemRequest.quantity());
        if (newReturnedQuantity.compareTo(sourceItem.getQuantity()) > 0) {
            throw new BadRequestException("Purchase return quantity exceeds received quantity for product");
        }
    }

    private BigDecimal getReturnedQuantityForProduct(UUID purchaseOrderId, UUID productId) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturn purchaseReturn : purchaseReturnRepository.findByPurchaseOrderId(purchaseOrderId)) {
            for (PurchaseReturnItem item : purchaseReturnItemRepository.findByPurchaseReturnId(purchaseReturn.getId())) {
                if (productId.equals(item.getProductId())) {
                    total = total.add(item.getQuantity());
                }
            }
        }
        return total;
    }

    private UUID resolveReceivedWarehouseId(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getReceivedWarehouseId() != null) {
            return purchaseOrder.getReceivedWarehouseId();
        }
        if (stockMovementRepository == null) {
            throw new BadRequestException("Purchase order has no received warehouse");
        }

        Set<UUID> candidateWarehouses = new LinkedHashSet<>(
                stockMovementRepository.findByReferenceId(purchaseOrder.getId()).stream()
                        .filter(movement -> "PURCHASE_ORDER".equals(movement.getReferenceType()))
                        .map(movement -> movement.getWarehouseId())
                        .toList());

        if (candidateWarehouses.isEmpty()) {
            throw new BadRequestException("Purchase order has no received warehouse");
        }
        if (candidateWarehouses.size() > 1) {
            throw new BadRequestException("Purchase order has multiple received warehouses");
        }

        UUID resolvedWarehouseId = candidateWarehouses.iterator().next();
        purchaseOrder.setReceivedWarehouseId(resolvedWarehouseId);
        purchaseOrderRepository.save(purchaseOrder);
        return resolvedWarehouseId;
    }

    private String normalizePerformedBy(String performedBy) {
        if (performedBy == null || performedBy.isBlank()) {
            throw new BadRequestException("performedBy is required");
        }
        return performedBy.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private String generatePurchaseReturnNumber() {
        String returnNumber;
        do {
            returnNumber = "PR-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (purchaseReturnRepository.findByReturnNumber(returnNumber).isPresent());

        return returnNumber;
    }
}
