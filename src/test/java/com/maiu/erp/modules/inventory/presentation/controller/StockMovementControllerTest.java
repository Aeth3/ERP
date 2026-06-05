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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.enums.MovementType;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class StockMovementControllerTest {

    private MockMvc mockMvc;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = Mockito.mock(InventoryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StockMovementController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStockMovementsReturnsEmptyListWhenNoMatchesExist() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryService.getStockMovements(productId, null, null, null, null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/inventory/stock-movements")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(inventoryService).getStockMovements(eq(productId), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void getStockMovementsReturnsOrderedMovementData() throws Exception {
        UUID movementId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        when(inventoryService.getStockMovements(productId, warehouseId, null, null, null, null, null, null))
                .thenReturn(List.of(movement(movementId, productId, warehouseId, referenceId)));

        mockMvc.perform(get("/inventory/stock-movements")
                        .param("productId", productId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(movementId.toString()))
                .andExpect(jsonPath("$[0].movementType").value("PURCHASE_IN"))
                .andExpect(jsonPath("$[0].quantity").value(12.5))
                .andExpect(jsonPath("$[0].referenceType").value("PURCHASE_ORDER"))
                .andExpect(jsonPath("$[0].referenceId").value(referenceId.toString()))
                .andExpect(jsonPath("$[0].performedBy").value("Alex Reyes"))
                .andExpect(jsonPath("$[0].movementDate").value("2026-05-22T08:00:00Z"));
    }

    @Test
    void getStockMovementsAcceptsReferenceTypeFilter() throws Exception {
        when(inventoryService.getStockMovements(null, null, null, null, "SALES_ORDER", null, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/inventory/stock-movements")
                        .param("referenceType", "SALES_ORDER"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(inventoryService).getStockMovements(eq(null), eq(null), eq(null), eq(null), eq("SALES_ORDER"), eq(null), eq(null), eq(null));
    }

    @Test
    void getStockMovementsAcceptsProjectMovementTypeAndDateFilters() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(inventoryService.getStockMovements(
                null,
                null,
                projectId,
                null,
                null,
                "PROJECT_ISSUE",
                LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-05-31"))).thenReturn(List.of());

        mockMvc.perform(get("/inventory/stock-movements")
                        .param("projectId", projectId.toString())
                        .param("movementType", "PROJECT_ISSUE")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(inventoryService).getStockMovements(
                eq(null),
                eq(null),
                eq(projectId),
                eq(null),
                eq(null),
                eq("PROJECT_ISSUE"),
                eq(LocalDate.parse("2026-05-01")),
                eq(LocalDate.parse("2026-05-31")));
    }

    private static StockMovement movement(
            UUID id,
            UUID productId,
            UUID warehouseId,
            UUID referenceId) {
        StockMovement movement = new StockMovement();
        movement.setId(id);
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementType(MovementType.PURCHASE_IN);
        movement.setQuantity(new BigDecimal("12.5"));
        movement.setUnitCost(new BigDecimal("42.25"));
        movement.setReferenceType("PURCHASE_ORDER");
        movement.setReferenceId(referenceId);
        movement.setRemarks("Initial receipt");
        movement.setPerformedBy("Alex Reyes");
        movement.setMovementDate(Instant.parse("2026-05-22T08:00:00Z"));
        return movement;
    }
}
