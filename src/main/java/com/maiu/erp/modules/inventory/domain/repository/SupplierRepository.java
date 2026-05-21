package com.maiu.erp.modules.inventory.domain.repository;

import java.util.*;
import com.maiu.erp.modules.inventory.domain.model.Supplier;

public interface SupplierRepository {
    Supplier save(Supplier supplier);

    Optional<Supplier> findById(UUID id);

    Optional<Supplier> findByCode(String code);

    List<Supplier> findAll();

    List<Supplier> findActiveSuppliers();
}
