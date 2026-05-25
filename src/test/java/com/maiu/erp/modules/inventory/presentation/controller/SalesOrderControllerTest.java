package com.maiu.erp.modules.inventory.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.maiu.erp.modules.inventory.application.service.SalesOrderService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class SalesOrderControllerTest {

    private MockMvc mockMvc;
    private SalesOrderService salesOrderService;

    @BeforeEach
    void setUp() {
        salesOrderService = Mockito.mock(SalesOrderService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SalesOrderController(salesOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createSalesOrderReturnsCreatedId() throws Exception {
        UUID salesOrderId = UUID.randomUUID();
        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrderId);

        mockMvc.perform(post("/inventory/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "11111111-1111-1111-1111-111111111111",
                                  "orderDate": "2026-05-21",
                                  "items": [
                                    {
                                      "productId": "22222222-2222-2222-2222-222222222222",
                                      "quantity": 2,
                                      "unitPrice": 10.50
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(salesOrderId.toString()));

        verify(salesOrderService).createSalesOrder(any());
    }

    @Test
    void cancelSalesOrderReturnsSuccessMessageForDraftOrder() throws Exception {
        UUID salesOrderId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/sales-orders/{id}/cancel", salesOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sales order cancelled successfully"));

        verify(salesOrderService).cancelSalesOrder(eq(salesOrderId), eq(null));
    }

    @Test
    void cancelSalesOrderReturnsSuccessMessageForConfirmedOrder() throws Exception {
        UUID salesOrderId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/sales-orders/{id}/cancel", salesOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": "%s"
                                }
                                """.formatted(warehouseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sales order cancelled successfully"));

        verify(salesOrderService).cancelSalesOrder(eq(salesOrderId), eq(warehouseId));
    }
}
