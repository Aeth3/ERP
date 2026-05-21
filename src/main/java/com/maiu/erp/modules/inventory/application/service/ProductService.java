package com.maiu.erp.modules.inventory.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.command.CreateProductCommand;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public UUID createProduct(CreateProductCommand command) {

        if (productRepository.existsBySku(command.sku())) {
            throw new RuntimeException("SKU already exists");
        }
        if (command.categoryId() != null) {
            categoryRepository.findById(command.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

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

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findActiveProducts();
    }

    public Product getProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}
