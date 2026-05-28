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

import com.maiu.erp.modules.inventory.application.service.WarehouseService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class WarehouseControllerTest {

    private MockMvc mockMvc;
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = Mockito.mock(WarehouseService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WarehouseController(warehouseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateWarehouseReturnsNoContent() throws Exception {
        UUID warehouseId = UUID.randomUUID();

        mockMvc.perform(put("/inventory/warehouses/{id}", warehouseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "MAIN-WH",
                                  "name": "Updated Warehouse",
                                  "active": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(warehouseService).updateWarehouse(eq(warehouseId), any());
    }

    @Test
    void deactivateWarehouseReturnsNoContent() throws Exception {
        UUID warehouseId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/warehouses/{id}/deactivate", warehouseId))
                .andExpect(status().isNoContent());

        verify(warehouseService).deactivateWarehouse(warehouseId);
    }

    @Test
    void activateWarehouseReturnsNoContent() throws Exception {
        UUID warehouseId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/warehouses/{id}/activate", warehouseId))
                .andExpect(status().isNoContent());

        verify(warehouseService).activateWarehouse(warehouseId);
    }
}
