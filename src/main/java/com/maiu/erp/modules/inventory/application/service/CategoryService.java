package com.maiu.erp.modules.inventory.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.domain.model.Category;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public UUID createCategory(Category category) {
        validateCategory(category.getName(), category.getParentId(), null);
        category.setActive(category.isActive());
        Category saved = categoryRepository.save(category);
        return saved.getId();
    }

    public void updateCategory(
            UUID categoryId,
            Category updatedCategory) {
        Category existingCategory = getCategoryById(categoryId);

        validateCategory(updatedCategory.getName(), updatedCategory.getParentId(), categoryId);

        existingCategory.setName(updatedCategory.getName());
        existingCategory.setParentId(updatedCategory.getParentId());
        existingCategory.setActive(updatedCategory.isActive());
        categoryRepository.save(existingCategory);
    }

    public void deactivateCategory(
            UUID categoryId) {
        Category category = getCategoryById(categoryId);

        if (!productRepository.findByCategoryId(categoryId).isEmpty()) {
            throw new BadRequestException("Category cannot be deactivated while products reference it");
        }

        category.setActive(false);
        categoryRepository.save(category);
    }

    public void activateCategory(
            UUID categoryId) {
        Category category = getCategoryById(categoryId);
        category.setActive(true);
        categoryRepository.save(category);
    }

    public List<Category> getCategories(boolean activeOnly) {
        return activeOnly
                ? categoryRepository.findActiveCategories()
                : categoryRepository.findAll();
    }

    public Category getCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void validateCategory(
            String name,
            UUID parentId,
            UUID categoryId) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category name is required");
        }

        if (parentId == null) {
            return;
        }

        if (categoryId != null && categoryId.equals(parentId)) {
            throw new BadRequestException("Category cannot be its own parent");
        }

        categoryRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent category not found"));
    }
}
