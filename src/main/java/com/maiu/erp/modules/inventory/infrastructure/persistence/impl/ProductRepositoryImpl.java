package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.ProductEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.ProductJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductRepositoryImpl
        implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductRepositoryImpl(
            ProductJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {

        ProductEntity entity = toEntity(product);

        ProductEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {

        return jpaRepository
                .findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Product> findBySku(String sku) {

        return jpaRepository
                .findBySku(sku)
                .map(this::toDomain);
    }

    @Override
    public List<Product> findByCategoryId(UUID categoryId) {
        return jpaRepository
                .findByCategoryId(categoryId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Product> findByUnitId(UUID unitId) {
        return jpaRepository
                .findByUnitId(unitId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Product> findAll() {

        return jpaRepository
                .findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Product> findActiveProducts() {

        return jpaRepository
                .findByActiveTrue()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {

        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsBySku(String sku) {

        return jpaRepository.existsBySku(sku);
    }

    private Product toDomain(
            ProductEntity entity) {

        Product product = new Product();

        product.setId(entity.getId());

        product.setSku(entity.getSku());

        product.setName(entity.getName());

        product.setDescription(
                entity.getDescription());

        product.setCostPrice(
                entity.getCostPrice());

        product.setSellingPrice(
                entity.getSellingPrice());

        product.setActive(
                entity.getActive());

        product.setCategoryId(
                entity.getCategoryId());

        product.setUnitId(
                entity.getUnitId());

        product.setCreatedAt(
                entity.getCreatedAt());

        product.setUpdatedAt(
                entity.getUpdatedAt());

        return product;
    }

    private ProductEntity toEntity(
            Product product) {

        ProductEntity entity =
                new ProductEntity();

        entity.setId(product.getId());

        entity.setSku(product.getSku());

        entity.setName(product.getName());

        entity.setDescription(
                product.getDescription());

        entity.setCostPrice(
                product.getCostPrice());

        entity.setSellingPrice(
                product.getSellingPrice());

        entity.setActive(
                product.getActive());

        entity.setCategoryId(
                product.getCategoryId());

        entity.setUnitId(
                product.getUnitId());

        entity.setCreatedAt(
                product.getCreatedAt());

        entity.setUpdatedAt(
                product.getUpdatedAt());

        return entity;
    }
}
