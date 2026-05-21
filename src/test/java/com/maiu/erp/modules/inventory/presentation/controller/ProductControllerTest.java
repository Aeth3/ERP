package com.maiu.erp.modules.inventory.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
