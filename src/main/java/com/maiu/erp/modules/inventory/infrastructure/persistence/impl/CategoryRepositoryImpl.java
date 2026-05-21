package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.inventory.domain.model.Category;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.infrastructure.persistence.entity.CategoryEntity;
import com.maiu.erp.modules.inventory.infrastructure.persistence.jpa.CategoryJpaRepository;

@Repository
public class CategoryRepositoryImpl
        implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    public CategoryRepositoryImpl(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        return toDomain(jpaRepository.save(toEntity(category)));
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Category> findActiveCategories() {
        return jpaRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Category> findByParentId(UUID parentId) {
        return jpaRepository.findByParentId(parentId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Category toDomain(CategoryEntity entity) {
        Category category = new Category();
        category.setId(entity.getId());
        category.setName(entity.getName());
        category.setParentId(entity.getParentId());
        category.setActive(entity.isActive());
        return category;
    }

    private CategoryEntity toEntity(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(category.getId());
        entity.setName(category.getName());
        entity.setParentId(category.getParentId());
        entity.setActive(category.isActive());
        return entity;
    }
}
