package com.volt.catalog.infrastructure.adapter.in.web.controller;

import com.volt.catalog.application.port.in.ListReferenceDataUseCase;
import com.volt.catalog.domain.model.Brand;
import com.volt.catalog.domain.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceDataControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new ReferenceDataController(new FakeReferenceData())).build();

    @Test
    void returnsCategoriesAndBrands() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CAB"));

        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Volt"));
    }

    private static final class FakeReferenceData implements ListReferenceDataUseCase {
        @Override
        public List<Category> listCategories() {
            return List.of(new Category(1L, "CAB", "Cables"));
        }

        @Override
        public List<Brand> listBrands() {
            return List.of(new Brand(1L, "Volt"));
        }
    }
}
