package com.maiu.erp.modules.inventory.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.command.CreateProductCommand;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.domain.repository.InventoryStockRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.UnitRepository;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.ConflictException;
import com.maiu.erp.shared.exception.NotFoundException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UnitRepository unitRepository,
            InventoryStockRepository inventoryStockRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    public UUID createProduct(CreateProductCommand command) {
        if (productRepository.existsBySku(command.sku())) {
            throw new ConflictException("SKU already exists");
        }

        validateCategoryExists(command.categoryId());
        validateUnitExists(command.unitId());

        Product product = new Product();
        product.setSku(command.sku());
        product.setName(command.name());
        product.setDescription(command.description());
        product.setCostPrice(command.costPrice());
        product.setSellingPrice(command.sellingPrice());
        product.setCategoryId(command.categoryId());
        product.setUnitId(command.unitId());
        product.setActive(true);
        product.setCreatedAt(Instant.now());

        Product saved = productRepository.save(product);
        return saved.getId();
    }

    public void updateProduct(
            UUID productId,
            Product updatedProduct) {
        Product existingProduct = getProductById(productId);

        productRepository.findBySku(updatedProduct.getSku())
                .filter(existing -> !productId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("SKU already exists");
                });

        validateCategoryExists(updatedProduct.getCategoryId());
        validateUnitExists(updatedProduct.getUnitId());

        existingProduct.setSku(updatedProduct.getSku());
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setCostPrice(updatedProduct.getCostPrice());
        existingProduct.setSellingPrice(updatedProduct.getSellingPrice());
        existingProduct.setCategoryId(updatedProduct.getCategoryId());
        existingProduct.setUnitId(updatedProduct.getUnitId());
        existingProduct.setActive(updatedProduct.getActive() == null || updatedProduct.getActive());
        existingProduct.setUpdatedAt(Instant.now());

        productRepository.save(existingProduct);
    }

    public void deactivateProduct(
            UUID productId) {
        Product product = getProductById(productId);

        boolean hasStock = inventoryStockRepository.findByProductId(productId)
                .stream()
                .anyMatch(this::hasInventoryBalance);

        if (hasStock) {
            throw new BadRequestException("Product cannot be deactivated while stock exists");
        }

        product.setActive(false);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findActiveProducts();
    }

    public Product getProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void validateCategoryExists(
            UUID categoryId) {
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }
    }

    private void validateUnitExists(
            UUID unitId) {
        if (unitId != null) {
            unitRepository.findById(unitId)
                    .orElseThrow(() -> new NotFoundException("Unit not found"));
        }
    }

    private boolean hasInventoryBalance(
            InventoryStock stock) {
        return stock.getQuantityOnHand() != null && stock.getQuantityOnHand().signum() > 0
                || stock.getReservedQuantity() != null && stock.getReservedQuantity().signum() > 0;
    }
}
