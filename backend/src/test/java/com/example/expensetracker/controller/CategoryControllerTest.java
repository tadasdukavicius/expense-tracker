package com.example.expensetracker.controller;

import com.example.expensetracker.config.SecurityConfig;
import com.example.expensetracker.dto.CategoryResponse;
import com.example.expensetracker.exception.ConflictException;
import com.example.expensetracker.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean CategoryService service;

    @Test
    void listReturnsCategories() throws Exception {
        when(service.list()).thenReturn(List.of(new CategoryResponse(1L, "Groceries", "#22aa55")));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Groceries"));
    }

    @Test
    void createReturns201() throws Exception {
        when(service.create(any())).thenReturn(new CategoryResponse(1L, "Groceries", "#22aa55"));

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Groceries\",\"color\":\"#22aa55\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createWithBlankNameReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        when(service.create(any())).thenThrow(new ConflictException("Category already exists: Groceries"));

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Groceries\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category already exists: Groceries"));
    }
}