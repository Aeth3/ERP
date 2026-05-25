package com.maiu.erp.modules.inventory.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.inventory.application.service.SupplierService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class SupplierControllerTest {

    private MockMvc mockMvc;
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierService = Mockito.mock(SupplierService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SupplierController(supplierService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateSupplierReturnsNoContent() throws Exception {
        UUID supplierId = UUID.randomUUID();

        mockMvc.perform(put("/inventory/suppliers/{id}", supplierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SUP-001",
                                  "name": "Updated Supplier",
                                  "active": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(supplierService).updateSupplier(eq(supplierId), any());
    }

    @Test
    void deactivateSupplierReturnsNoContent() throws Exception {
        UUID supplierId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/suppliers/{id}/deactivate", supplierId))
                .andExpect(status().isNoContent());

        verify(supplierService).deactivateSupplier(supplierId);
    }
}
