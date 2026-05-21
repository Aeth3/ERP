package com.maiu.erp.modules.inventory.domain.repository;

import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import java.util.*;

public interface WarehouseRepository {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(UUID id);

    Optional<Warehouse> findByCode(String code);

    List<Warehouse> findAll();

    List<Warehouse> findActiveWarehouses();
}
