package com.maiu.erp.modules.inventory.presentation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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

    @Test
    void getStocksReturnsAllStocksWhenNoFilterIsProvided() throws Exception {
        UUID stockId = UUID.randomUUID();
        InventoryStock stock = stock(stockId, UUID.randomUUID(), UUID.randomUUID(), "15", "4");
        when(inventoryService.getAllStocks()).thenReturn(List.of(stock));

        mockMvc.perform(get("/inventory/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(stockId.toString()))
                .andExpect(jsonPath("$[0].quantityOnHand").value(15))
                .andExpect(jsonPath("$[0].reservedQuantity").value(4));

        verify(inventoryService).getAllStocks();
    }

    @Test
    void createStockAdjustmentReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/inventory/stocks/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "11111111-1111-1111-1111-111111111111",
                                  "warehouseId": "22222222-2222-2222-2222-222222222222",
                                  "adjustmentType": "INCREASE",
                                  "quantity": 5.5,
                                  "unitCost": 12.25,
                                  "reason": "Cycle count correction",
                                  "notes": "Found extra stock on shelf A",
                                  "performedBy": "Alex Reyes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stock adjusted successfully"));

        verify(inventoryService).adjustStock(
                eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                eq(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                eq("INCREASE"),
                eq(new BigDecimal("5.5")),
                eq(new BigDecimal("12.25")),
                eq("Cycle count correction"),
                eq("Found extra stock on shelf A"),
                eq("Alex Reyes"));
    }

    @Test
    void createStockAdjustmentReturnsBadRequestWhenReasonMissing() throws Exception {
        mockMvc.perform(post("/inventory/stocks/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "11111111-1111-1111-1111-111111111111",
                                  "warehouseId": "22222222-2222-2222-2222-222222222222",
                                  "adjustmentType": "DECREASE",
                                  "quantity": 2,
                                  "performedBy": "Alex Reyes"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("reason is required"));
    }

    @Test
    void createStockTransferReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/inventory/stocks/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "11111111-1111-1111-1111-111111111111",
                                  "fromWarehouseId": "22222222-2222-2222-2222-222222222222",
                                  "toWarehouseId": "33333333-3333-3333-3333-333333333333",
                                  "quantity": 3,
                                  "unitCost": 12.25,
                                  "reason": "Rebalancing stock",
                                  "notes": "Move excess stock to overflow warehouse",
                                  "performedBy": "Alex Reyes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stock transferred successfully"));

        verify(inventoryService).transferStock(
                eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                eq(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                eq(UUID.fromString("33333333-3333-3333-3333-333333333333")),
                eq(new BigDecimal("3")),
                eq(new BigDecimal("12.25")),
                eq("Rebalancing stock"),
                eq("Move excess stock to overflow warehouse"),
                eq("Alex Reyes"));
    }

    @Test
    void createStockTransferReturnsBadRequestWhenReasonMissing() throws Exception {
        mockMvc.perform(post("/inventory/stocks/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "11111111-1111-1111-1111-111111111111",
                                  "fromWarehouseId": "22222222-2222-2222-2222-222222222222",
                                  "toWarehouseId": "33333333-3333-3333-3333-333333333333",
                                  "quantity": 2,
                                  "performedBy": "Alex Reyes"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("reason is required"));
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
