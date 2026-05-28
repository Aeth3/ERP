package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.command.CreateProductCommand;
import com.maiu.erp.modules.inventory.application.dto.CreateProductRequest;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.dto.ProductDto;
import com.maiu.erp.modules.inventory.application.dto.UpdateProductRequest;
import com.maiu.erp.modules.inventory.application.service.ProductService;
import com.maiu.erp.modules.inventory.domain.model.Product;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        UUID productId = productService.createProduct(
                new CreateProductCommand(
                        request.sku(),
                        request.name(),
                        request.description(),
                        request.costPrice(),
                        request.sellingPrice(),
                        request.categoryId(),
                        request.unitId()));

        return ResponseEntity.status(201).body(new IdResponse(productId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCostPrice(request.costPrice());
        product.setSellingPrice(request.sellingPrice());
        product.setCategoryId(request.categoryId());
        product.setUnitId(request.unitId());
        product.setActive(request.active() == null || request.active());

        productService.updateProduct(id, product);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable UUID id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateProduct(
            @PathVariable UUID id) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<Product> products = activeOnly
                ? productService.getActiveProducts()
                : productService.getProducts();

        return ResponseEntity.ok(
                products.stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(productService.getProductById(id)));
    }

    private ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCostPrice(),
                product.getSellingPrice(),
                product.getActive(),
                product.getCategoryId(),
                product.getUnitId(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
