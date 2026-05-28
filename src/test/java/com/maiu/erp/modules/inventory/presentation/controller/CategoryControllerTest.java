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

import com.maiu.erp.modules.inventory.application.service.CategoryService;
import com.maiu.erp.shared.exception.GlobalExceptionHandler;

class CategoryControllerTest {

    private MockMvc mockMvc;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = Mockito.mock(CategoryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CategoryController(categoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateCategoryReturnsNoContent() throws Exception {
        UUID categoryId = UUID.randomUUID();

        mockMvc.perform(put("/inventory/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Category",
                                  "active": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryService).updateCategory(eq(categoryId), any());
    }

    @Test
    void deactivateCategoryReturnsNoContent() throws Exception {
        UUID categoryId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/categories/{id}/deactivate", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService).deactivateCategory(categoryId);
    }

    @Test
    void activateCategoryReturnsNoContent() throws Exception {
        UUID categoryId = UUID.randomUUID();

        mockMvc.perform(post("/inventory/categories/{id}/activate", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService).activateCategory(categoryId);
    }
}
