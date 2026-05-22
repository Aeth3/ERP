package com.maiu.erp.modules.inventory.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class InventoryStockControllerTest {

    private MockMvc mockMvc;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = Mockito.mock(InventoryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InventoryStockController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStocksReturnsEmptyListWhenExactStockDoesNotExist() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        when(inventoryService.findStock(productId, warehouseId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/inventory/stocks")
                        .param("productId", productId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(inventoryService).findStock(eq(productId), eq(warehouseId));
    }

    @Test
    void getStocksReturnsListWhenFilteringByProduct() throws Exception {
        UUID stockId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        InventoryStock stock = stock(stockId, productId, warehouseId, "10", "2");
        when(inventoryService.getStocksByProduct(productId)).thenReturn(List.of(stock));

        mockMvc.perform(get("/inventory/stocks")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(stockId.toString()))
                .andExpect(jsonPath("$[0].availableQuantity").value(8));
    }

    private static InventoryStock stock(
            UUID id,
            UUID productId,
            UUID warehouseId,
            String quantityOnHand,
            String reservedQuantity) {
        InventoryStock stock = new InventoryStock();
        stock.setId(id);
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantityOnHand(new BigDecimal(quantityOnHand));
        stock.setReservedQuantity(new BigDecimal(reservedQuantity));
        stock.setReorderLevel(BigDecimal.ONE);
        stock.setUpdatedAt(Instant.parse("2026-05-22T08:00:00Z"));
        return stock;
    }
}
