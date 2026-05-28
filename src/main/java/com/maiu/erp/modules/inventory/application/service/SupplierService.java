package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierService(
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public UUID createSupplier(Supplier supplier) {
        validateSupplier(supplier, null);
        if (supplier.getActive() == null) {
            supplier.setActive(true);
        }
        Supplier saved = supplierRepository.save(supplier);
        return saved.getId();
    }

    public void updateSupplier(
            UUID supplierId,
            Supplier updatedSupplier) {
        Supplier existingSupplier = getSupplierById(supplierId);
        validateSupplier(updatedSupplier, supplierId);

        existingSupplier.setCode(updatedSupplier.getCode());
        existingSupplier.setName(updatedSupplier.getName());
        existingSupplier.setContactPerson(updatedSupplier.getContactPerson());
        existingSupplier.setPhone(updatedSupplier.getPhone());
        existingSupplier.setEmail(updatedSupplier.getEmail());
        existingSupplier.setAddress(updatedSupplier.getAddress());
        existingSupplier.setActive(updatedSupplier.getActive() == null || updatedSupplier.getActive());
        supplierRepository.save(existingSupplier);
    }

    public void deactivateSupplier(
            UUID supplierId) {
        Supplier supplier = getSupplierById(supplierId);

        boolean hasActivePurchaseOrders = purchaseOrderRepository.findBySupplierId(supplierId)
                .stream()
                .anyMatch(order -> order.getStatus() != PurchaseOrderStatus.CANCELLED);

        if (hasActivePurchaseOrders) {
            throw new BadRequestException("Supplier cannot be deactivated while purchase orders reference it");
        }

        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    public void activateSupplier(
            UUID supplierId) {
        Supplier supplier = getSupplierById(supplierId);
        supplier.setActive(true);
        supplierRepository.save(supplier);
    }

    public List<Supplier> getSuppliers(boolean activeOnly) {
        return activeOnly
                ? supplierRepository.findActiveSuppliers()
                : supplierRepository.findAll();
    }

    public Supplier getSupplierById(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
    }

    private void validateSupplier(
            Supplier supplier,
            UUID supplierId) {
        if (supplier.getCode() == null || supplier.getCode().isBlank()) {
            throw new BadRequestException("Supplier code is required");
        }
        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new BadRequestException("Supplier name is required");
        }

        supplierRepository.findByCode(supplier.getCode())
                .filter(existing -> supplierId == null || !supplierId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Supplier code already exists");
                });
    }
}
