package com.maiu.erp.modules.inventory.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.inventory.application.service.ProductService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class ProductControllerTest {

    private MockMvc mockMvc;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = Mockito.mock(ProductService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createProductReturnsCreatedId() throws Exception {
        UUID productId = UUID.randomUUID();
        when(productService.createProduct(any())).thenReturn(productId);

        mockMvc.perform(post("/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-001",
                                  "name": "Brake Pad",
                                  "description": "Front brake pad",
                                  "costPrice": 100.00,
                                  "sellingPrice": 150.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId.toString()));

        verify(productService).createProduct(any());
    }

    @Test
    void createProductReturnsBadRequestWhenSkuMissing() throws Exception {
        mockMvc.perform(post("/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Brake Pad",
                                  "costPrice": 100.00,
                                  "sellingPrice": 150.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sku").value("sku is required"));
    }

    @Test
    void updateProductReturnsNoContent() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(put("/inventory/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-002",
                                  "name": "Updated Brake Pad",
                                  "description": "Updated description",
                                  "costPrice": 120.00,
                                  "sellingPrice": 180.00,
                                  "active": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(productService).updateProduct(eq(productId), any());
    }

    @Test
    void deactivateProductReturnsNoContent() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/products/{id}/deactivate", productId))
                .andExpect(status().isNoContent());

        verify(productService).deactivateProduct(productId);
    }

    @Test
    void activateProductReturnsNoContent() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/products/{id}/activate", productId))
                .andExpect(status().isNoContent());

        verify(productService).activateProduct(productId);
    }
}
