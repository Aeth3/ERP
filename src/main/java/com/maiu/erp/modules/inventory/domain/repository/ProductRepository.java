package com.maiu.erp.modules.inventory.domain.repository;

import java.util.*;

import com.maiu.erp.modules.inventory.domain.model.Product;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    List<Product> findAll();

    List<Product> findActiveProducts();

    void deleteById(UUID id);

    boolean existsBySku(String sku);
}
