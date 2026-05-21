package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public UUID createSupplier(Supplier supplier) {
        if (supplier.getCode() == null || supplier.getCode().isBlank()) {
            throw new RuntimeException("Supplier code is required");
        }
        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new RuntimeException("Supplier name is required");
        }
        if (supplierRepository.findByCode(supplier.getCode()).isPresent()) {
            throw new RuntimeException("Supplier code already exists");
        }
        if (supplier.getActive() == null) {
            supplier.setActive(true);
        }
        Supplier saved = supplierRepository.save(supplier);
        return saved.getId();
    }

    public List<Supplier> getSuppliers(boolean activeOnly) {
        return activeOnly
                ? supplierRepository.findActiveSuppliers()
                : supplierRepository.findAll();
    }

    public Supplier getSupplierById(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
    }
}
