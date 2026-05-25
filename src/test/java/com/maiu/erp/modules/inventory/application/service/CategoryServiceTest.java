package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.domain.model.Category;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.shared.exception.BadRequestException;

class CategoryServiceTest {

    @Test
    void deactivateCategoryFailsWhenProductsReferenceIt() {
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        CategoryService categoryService = new CategoryService(categoryRepository, productRepository);

        Category category = new Category();
        category.setName("Spare Parts");
        category.setActive(true);
        UUID categoryId = categoryService.createCategory(category);

        Product product = new Product();
        product.setSku("SKU-001");
        product.setName("Brake Pad");
        product.setActive(true);
        product.setCategoryId(categoryId);
        productRepository.save(product);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> categoryService.deactivateCategory(categoryId));

        assertEquals("Category cannot be deactivated while products reference it", exception.getMessage());
    }

    private static final class InMemoryCategoryRepository implements CategoryRepository {
        private final Map<UUID, Category> categories = new HashMap<>();

        @Override
        public Category save(Category category) {
            if (category.getId() == null) {
                category.setId(UUID.randomUUID());
            }
            categories.put(category.getId(), category);
            return category;
        }

        @Override
        public Optional<Category> findById(UUID id) {
            return Optional.ofNullable(categories.get(id));
        }

        @Override
        public List<Category> findAll() {
            return categories.values().stream().toList();
        }

        @Override
        public List<Category> findActiveCategories() {
            return categories.values().stream().filter(Category::isActive).toList();
        }

        @Override
        public List<Category> findByParentId(UUID parentId) {
            return categories.values().stream()
                    .filter(category -> parentId.equals(category.getParentId()))
                    .toList();
        }
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private final Map<UUID, Product> products = new HashMap<>();

        @Override
        public Product save(Product product) {
            if (product.getId() == null) {
                product.setId(UUID.randomUUID());
            }
            products.put(product.getId(), product);
            return product;
        }

        @Override
        public Optional<Product> findById(UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public Optional<Product> findBySku(String sku) {
            return products.values().stream().filter(product -> sku.equals(product.getSku())).findFirst();
        }

        @Override
        public List<Product> findByCategoryId(UUID categoryId) {
            return products.values().stream()
                    .filter(product -> categoryId.equals(product.getCategoryId()))
                    .toList();
        }

        @Override
        public List<Product> findByUnitId(UUID unitId) {
            return List.of();
        }

        @Override
        public List<Product> findAll() {
            return products.values().stream().toList();
        }

        @Override
        public List<Product> findActiveProducts() {
            return products.values().stream().filter(product -> Boolean.TRUE.equals(product.getActive())).toList();
        }

        @Override
        public void deleteById(UUID id) {
            products.remove(id);
        }

        @Override
        public boolean existsBySku(String sku) {
            return products.values().stream().anyMatch(product -> sku.equals(product.getSku()));
        }
    }
}
