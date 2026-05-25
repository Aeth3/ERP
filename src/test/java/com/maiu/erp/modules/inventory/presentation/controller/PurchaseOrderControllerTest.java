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

import com.maiu.erp.modules.inventory.application.service.PurchaseOrderService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class PurchaseOrderControllerTest {

    private MockMvc mockMvc;
    private PurchaseOrderService purchaseOrderService;

    @BeforeEach
    void setUp() {
        purchaseOrderService = Mockito.mock(PurchaseOrderService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PurchaseOrderController(purchaseOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPurchaseOrderReturnsCreatedId() throws Exception {
        UUID purchaseOrderId = UUID.randomUUID();
        when(purchaseOrderService.createPurchaseOrder(any())).thenReturn(purchaseOrderId);

        mockMvc.perform(post("/inventory/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "11111111-1111-1111-1111-111111111111",
                                  "orderDate": "2026-05-21",
                                  "expectedDate": "2026-05-28",
                                  "items": [
                                    {
                                      "productId": "22222222-2222-2222-2222-222222222222",
                                      "quantity": 2,
                                      "unitCost": 10.50
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(purchaseOrderId.toString()));

        verify(purchaseOrderService).createPurchaseOrder(any());
    }

    @Test
    void approvePurchaseOrderReturnsSuccessMessage() throws Exception {
        UUID purchaseOrderId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/purchase-orders/{id}/approve", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Purchase order approved successfully"));

        verify(purchaseOrderService).approvePurchaseOrder(eq(purchaseOrderId));
    }

    @Test
    void cancelPurchaseOrderReturnsSuccessMessage() throws Exception {
        UUID purchaseOrderId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/purchase-orders/{id}/cancel", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Purchase order cancelled successfully"));

        verify(purchaseOrderService).cancelPurchaseOrder(eq(purchaseOrderId));
    }

    @Test
    void createPurchaseOrderReturnsBadRequestWhenItemsMissing() throws Exception {
        mockMvc.perform(post("/inventory/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "11111111-1111-1111-1111-111111111111",
                                  "orderDate": "2026-05-21",
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.items").value("items are required"));
    }
}
