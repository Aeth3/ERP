package com.maiu.erp.modules.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.maiu.erp.modules.inventory.application.command.CreateProductCommand;
import com.maiu.erp.modules.inventory.domain.model.Category;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.Unit;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.UnitRepository;

class ProductServiceTest {

    @Test
    void createProductPersistsProductWhenCategoryExists() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryUnitRepository unitRepository = new InMemoryUnitRepository();
        UUID categoryId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        categoryRepository.save(category(categoryId, "Spare Parts"));
        unitRepository.save(unit(unitId, "Piece", "pc"));

        ProductService productService = new ProductService(
                productRepository,
                categoryRepository,
                unitRepository);

        UUID productId = productService.createProduct(
                new CreateProductCommand(
                        "SKU-001",
                        "Brake Pad",
                        "Front brake pad",
                        new BigDecimal("100.00"),
                        new BigDecimal("150.00"),
                        categoryId,
                        unitId));

        Product product = productRepository.findById(productId).orElseThrow();
        assertNotNull(product.getId());
        assertEquals("SKU-001", product.getSku());
        assertEquals(categoryId, product.getCategoryId());
        assertEquals(unitId, product.getUnitId());
    }

    @Test
    void createProductFailsWhenCategoryDoesNotExist() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryUnitRepository unitRepository = new InMemoryUnitRepository();

        ProductService productService = new ProductService(
                productRepository,
                categoryRepository,
                unitRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productService.createProduct(
                        new CreateProductCommand(
                                "SKU-001",
                                "Brake Pad",
                                "Front brake pad",
                                new BigDecimal("100.00"),
                                new BigDecimal("150.00"),
                                UUID.randomUUID(),
                                null)));

        assertEquals("Category not found", exception.getMessage());
    }

    @Test
    void createProductFailsWhenUnitDoesNotExist() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryUnitRepository unitRepository = new InMemoryUnitRepository();

        ProductService productService = new ProductService(
                productRepository,
                categoryRepository,
                unitRepository);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productService.createProduct(
                        new CreateProductCommand(
                                "SKU-001",
                                "Brake Pad",
                                "Front brake pad",
                                new BigDecimal("100.00"),
                                new BigDecimal("150.00"),
                                null,
                                UUID.randomUUID())));

        assertEquals("Unit not found", exception.getMessage());
    }

    private static Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setActive(true);
        return category;
    }

    private static Unit unit(UUID id, String name, String symbol) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setName(name);
        unit.setSymbol(symbol);
        unit.setActive(true);
        return unit;
    }

    private static final class InMemoryProductRepository
            implements ProductRepository {
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
            return products.values().stream()
                    .filter(product -> sku.equals(product.getSku()))
                    .findFirst();
        }

        @Override
        public List<Product> findAll() {
            return products.values().stream().toList();
        }

        @Override
        public List<Product> findActiveProducts() {
            return products.values().stream()
                    .filter(product -> Boolean.TRUE.equals(product.getActive()))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            products.remove(id);
        }

        @Override
        public boolean existsBySku(String sku) {
            return products.values().stream()
                    .anyMatch(product -> sku.equals(product.getSku()));
        }
    }

    private static final class InMemoryCategoryRepository
            implements CategoryRepository {
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
            return categories.values().stream()
                    .filter(Category::isActive)
                    .toList();
        }

        @Override
        public List<Category> findByParentId(UUID parentId) {
            return categories.values().stream()
                    .filter(category -> parentId.equals(category.getParentId()))
                    .toList();
        }
    }

    private static final class InMemoryUnitRepository
            implements UnitRepository {
        private final Map<UUID, Unit> units = new HashMap<>();

        @Override
        public Unit save(Unit unit) {
            if (unit.getId() == null) {
                unit.setId(UUID.randomUUID());
            }
            units.put(unit.getId(), unit);
            return unit;
        }

        @Override
        public Optional<Unit> findById(UUID id) {
            return Optional.ofNullable(units.get(id));
        }

        @Override
        public Optional<Unit> findByName(String name) {
            return units.values().stream()
                    .filter(unit -> name.equalsIgnoreCase(unit.getName()))
                    .findFirst();
        }

        @Override
        public List<Unit> findAll() {
            return units.values().stream().toList();
        }

        @Override
        public List<Unit> findActiveUnits() {
            return units.values().stream()
                    .filter(Unit::isActive)
                    .toList();
        }
    }
}
