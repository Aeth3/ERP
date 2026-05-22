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

import com.maiu.erp.modules.inventory.application.service.UnitService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class UnitControllerTest {

    private MockMvc mockMvc;
    private UnitService unitService;

    @BeforeEach
    void setUp() {
        unitService = Mockito.mock(UnitService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UnitController(unitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUnitReturnsCreatedId() throws Exception {
        UUID unitId = UUID.randomUUID();
        when(unitService.createUnit(any())).thenReturn(unitId);

        mockMvc.perform(post("/inventory/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Piece",
                                  "symbol": "pc",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(unitId.toString()));

        verify(unitService).createUnit(any());
    }

    @Test
    void createUnitReturnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(post("/inventory/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "symbol": "pc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("name is required"));
    }
}
