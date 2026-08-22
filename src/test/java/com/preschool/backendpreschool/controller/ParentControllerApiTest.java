package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.ParentResponse;
import com.preschool.backendpreschool.model.ParentStatus;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.ParentService;
import com.preschool.backendpreschool.service.StudentGuardianService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(ParentController.class)
@Import({SecurityConfig.class, ParentControllerApiTest.SecurityTestConfig.class})
class ParentControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParentService parentService;

    @MockitoBean
    private StudentGuardianService studentGuardianService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListParentsWithFilters() throws Exception {
        when(parentService.getAllParents(ParentStatus.ACTIVE, "ana", null)).thenReturn(List.of(parent()));

        mockMvc.perform(get("/api/parents")
                        .param("status", "ACTIVE")
                        .param("search", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentId").value(1))
                .andExpect(jsonPath("$[0].email").value("ana.parent@example.com"));

        verify(parentService).getAllParents(ParentStatus.ACTIVE, "ana", null);
    }

    @Test
    @WithMockUser(username = "ana.parent@example.com", roles = "PARENT")
    void parentCanReadOwnProfileOnly() throws Exception {
        when(parentService.getCurrentParent("ana.parent@example.com")).thenReturn(parent());

        mockMvc.perform(get("/api/parents/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(1));
        mockMvc.perform(get("/api/parents"))
                .andExpect(status().isForbidden());

        verify(parentService).getCurrentParent("ana.parent@example.com");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadSingleParentAndLinkedStudentsOnly() throws Exception {
        when(parentService.getParentById(1L)).thenReturn(parent());
        when(studentGuardianService.getGuardiansByParent(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/parents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(1));
        mockMvc.perform(get("/api/parents/1/students"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/parents"))
                .andExpect(status().isForbidden());

        verify(parentService).getParentById(1L);
        verify(studentGuardianService).getGuardiansByParent(1L);
    }

    @Test
    void unauthenticatedUserCannotAccessParents() throws Exception {
        mockMvc.perform(get("/api/parents/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteAndRestoreParent() throws Exception {
        when(parentService.restoreParent(1L)).thenReturn(parent());

        mockMvc.perform(delete("/api/parents/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/parents/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(1));

        verify(parentService).deleteParent(1L);
        verify(parentService).restoreParent(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanClaimArchivedParent() throws Exception {
        when(parentService.claimParent(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(parent());

        mockMvc.perform(post("/api/parents/1/claim")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ana",
                                  "lastName": "Parent",
                                  "email": "ana.parent@example.com",
                                  "phone": "0700000000",
                                  "address": "Street 1",
                                  "preferredLanguage": "sv"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(1));

        verify(parentService).claimParent(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
    }

    private ParentResponse parent() {
        return new ParentResponse(
                1L,
                2L,
                "Ana",
                "Parent",
                "ana.parent@example.com",
                "0700000000",
                "Street 1",
                "sv",
                ParentStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );
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
