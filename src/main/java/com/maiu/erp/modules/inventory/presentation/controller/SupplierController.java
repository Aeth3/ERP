package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.CreateSupplierRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.SupplierDto;
import com.maiu.erp.modules.inventory.application.dto.UpdateSupplierRequest;
import com.maiu.erp.modules.inventory.application.service.SupplierService;
import com.maiu.erp.modules.inventory.domain.model.Supplier;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request) {
        Supplier supplier = toModel(request);

        UUID supplierId = supplierService.createSupplier(supplier);

        return ResponseEntity.status(201).body(new IdResponse(supplierId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        supplierService.updateSupplier(id, toModel(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateSupplier(
            @PathVariable UUID id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateSupplier(
            @PathVariable UUID id) {
        supplierService.activateSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SupplierDto>> getSuppliers(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(
                supplierService.getSuppliers(activeOnly).stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(supplierService.getSupplierById(id)));
    }

    private Supplier toModel(CreateSupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setActive(request.active() == null || request.active());
        return supplier;
    }

    private Supplier toModel(UpdateSupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setActive(request.active() == null || request.active());
        return supplier;
    }

    private SupplierDto toDto(Supplier supplier) {
        return new SupplierDto(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getContactPerson(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getActive());
    }
}
