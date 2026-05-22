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

import com.maiu.erp.modules.inventory.application.service.CustomerService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class CustomerControllerTest {

    private MockMvc mockMvc;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = Mockito.mock(CustomerService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CustomerController(customerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCustomerReturnsCreatedId() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerService.createCustomer(any())).thenReturn(customerId);

        mockMvc.perform(post("/inventory/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CUS-001",
                                  "name": "Acme Corp",
                                  "email": "billing@acme.test",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()));

        verify(customerService).createCustomer(any());
    }

    @Test
    void createCustomerReturnsBadRequestWhenCodeMissing() throws Exception {
        mockMvc.perform(post("/inventory/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Corp"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("code is required"));
    }
}
