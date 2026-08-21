package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.MaterialResponse;
import com.preschool.backendpreschool.dto.MaterialSuggestedMinimumResponse;
import com.preschool.backendpreschool.model.MaterialConsumptionWindow;
import com.preschool.backendpreschool.model.MaterialStatus;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialController.class)
@Import({SecurityConfig.class, MaterialControllerApiTest.SecurityTestConfig.class})
class MaterialControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService materialService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadMaterialsButCannotCreate() throws Exception {
        when(materialService.getMaterials("paper", "classroom", MaterialStatus.ACTIVE, true, null))
                .thenReturn(List.of(material()));

        mockMvc.perform(get("/api/materials")
                        .param("search", "paper")
                        .param("category", "classroom")
                        .param("status", "ACTIVE")
                        .param("lowStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materialId").value(1))
                .andExpect(jsonPath("$[0].lowStock").value(true));
        mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMaterialRequest()))
                .andExpect(status().isForbidden());

        verify(materialService).getMaterials("paper", "classroom", MaterialStatus.ACTIVE, true, null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateMaterial() throws Exception {
        when(materialService.createMaterial(org.mockito.ArgumentMatchers.any())).thenReturn(material());

        mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMaterialRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.materialId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteAndRestoreMaterial() throws Exception {
        when(materialService.restoreMaterial(1L)).thenReturn(material());

        mockMvc.perform(delete("/api/materials/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/materials/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materialId").value(1));

        verify(materialService).deleteMaterial(1L);
        verify(materialService).restoreMaterial(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanGetSuggestedMinimum() throws Exception {
        when(materialService.getSuggestedMinimum(1L, MaterialConsumptionWindow.THREE_MONTHS)).thenReturn(
                new MaterialSuggestedMinimumResponse(1L, 5, MaterialConsumptionWindow.THREE_MONTHS, 12.0, 12, true)
        );

        mockMvc.perform(get("/api/materials/1/suggested-minimum")
                        .param("window", "THREE_MONTHS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedMinimumQuantity").value(12))
                .andExpect(jsonPath("$.hasData").value(true));

        verify(materialService).getSuggestedMinimum(1L, MaterialConsumptionWindow.THREE_MONTHS);
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotAccessMaterials() throws Exception {
        mockMvc.perform(get("/api/materials"))
                .andExpect(status().isForbidden());
    }

    private MaterialResponse material() {
        return new MaterialResponse(
                1L,
                "MAT-001",
                "Paper",
                "classroom",
                "pack",
                2,
                5,
                true,
                MaterialStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private String validMaterialRequest() {
        return """
                {
                  "sku": "MAT-001",
                  "name": "Paper",
                  "category": "classroom",
                  "unit": "pack",
                  "quantityOnHand": 2,
                  "minimumQuantity": 5,
                  "status": "ACTIVE"
                }
                """;
    }

    static class SecurityTestConfig {

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService,
                CustomUserDetailsService customUserDetailsService
        ) {
            return new JwtAuthenticationFilter(jwtService, customUserDetailsService);
        }
    }
}
