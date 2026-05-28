package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class SupplierServiceTest {

    @Test
    void deactivateSupplierFailsWhenPurchaseOrdersReferenceIt() {
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        SupplierService supplierService = new SupplierService(supplierRepository, purchaseOrderRepository);

        Supplier supplier = new Supplier();
        supplier.setCode("SUP-001");
        supplier.setName("Supplier");
        supplier.setActive(true);
        UUID supplierId = supplierService.createSupplier(supplier);

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(supplierId);
        order.setPoNumber("PO-001");
        order.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(order);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> supplierService.deactivateSupplier(supplierId));

        assertEquals("Supplier cannot be deactivated while purchase orders reference it", exception.getMessage());
    }

    @Test
    void activateSupplierMarksSupplierActive() {
        InMemorySupplierRepository supplierRepository = new InMemorySupplierRepository();
        InMemoryPurchaseOrderRepository purchaseOrderRepository = new InMemoryPurchaseOrderRepository();
        SupplierService supplierService = new SupplierService(supplierRepository, purchaseOrderRepository);

        Supplier supplier = new Supplier();
        supplier.setCode("SUP-001");
        supplier.setName("Supplier");
        supplier.setActive(false);
        UUID supplierId = supplierService.createSupplier(supplier);

        supplierService.activateSupplier(supplierId);

        assertTrue(supplierRepository.findById(supplierId).orElseThrow().getActive());
    }

    private static final class InMemorySupplierRepository implements SupplierRepository {
        private final Map<UUID, Supplier> suppliers = new HashMap<>();

        @Override
        public Supplier save(Supplier supplier) {
            if (supplier.getId() == null) {
                supplier.setId(UUID.randomUUID());
            }
            suppliers.put(supplier.getId(), supplier);
            return supplier;
        }

        @Override
        public Optional<Supplier> findById(UUID id) {
            return Optional.ofNullable(suppliers.get(id));
        }

        @Override
        public Optional<Supplier> findByCode(String code) {
            return suppliers.values().stream().filter(supplier -> code.equals(supplier.getCode())).findFirst();
        }

        @Override
        public List<Supplier> findAll() {
            return suppliers.values().stream().toList();
        }

        @Override
        public List<Supplier> findActiveSuppliers() {
            return suppliers.values().stream().filter(supplier -> Boolean.TRUE.equals(supplier.getActive())).toList();
        }
    }

    private static final class InMemoryPurchaseOrderRepository implements PurchaseOrderRepository {
        private final Map<UUID, PurchaseOrder> purchaseOrders = new HashMap<>();

        @Override
        public PurchaseOrder save(PurchaseOrder purchaseOrder) {
            if (purchaseOrder.getId() == null) {
                purchaseOrder.setId(UUID.randomUUID());
            }
            purchaseOrders.put(purchaseOrder.getId(), purchaseOrder);
            return purchaseOrder;
        }

        @Override
        public Optional<PurchaseOrder> findById(UUID id) {
            return Optional.ofNullable(purchaseOrders.get(id));
        }

        @Override
        public Optional<PurchaseOrder> findByPoNumber(String poNumber) {
            return purchaseOrders.values().stream().filter(order -> poNumber.equals(order.getPoNumber())).findFirst();
        }

        @Override
        public List<PurchaseOrder> findAll() {
            return purchaseOrders.values().stream().toList();
        }

        @Override
        public List<PurchaseOrder> findBySupplierId(UUID supplierId) {
            return purchaseOrders.values().stream()
                    .filter(order -> supplierId.equals(order.getSupplierId()))
                    .toList();
        }

        @Override
        public List<PurchaseOrder> findByProjectId(UUID projectId) {
            return purchaseOrders.values().stream()
                    .filter(order -> projectId.equals(order.getProjectId()))
                    .toList();
        }
    }
}
