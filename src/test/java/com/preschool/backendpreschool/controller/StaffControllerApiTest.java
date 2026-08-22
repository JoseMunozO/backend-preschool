package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.StaffResponse;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.StaffService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@Import({SecurityConfig.class, StaffControllerApiTest.SecurityTestConfig.class})
class StaffControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffService staffService;

    @Test
    @WithMockUser(username = "admin@school.com", roles = "ADMIN")
    void adminCanListAndCreateStaff() throws Exception {
        when(staffService.getAllStaff()).thenReturn(List.of(staff()));
        when(staffService.getStaffById(1L)).thenReturn(staff());
        when(staffService.createStaff(any(), eq("admin@school.com"))).thenReturn(staff());

        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffId").value(1));

        mockMvc.perform(get("/api/staff/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sara"));

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sara",
                                  "lastName": "Assistant",
                                  "positionTitle": "Assistant Teacher",
                                  "staffType": "teacher"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffId").value(1));

        verify(staffService).createStaff(any(), eq("admin@school.com"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAccessStaff() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessStaff() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isUnauthorized());
    }

    private StaffResponse staff() {
        return new StaffResponse(
                1L, null, "STAFF-010", "Sara", "Assistant", null, null,
                "Assistant Teacher", "teacher", null, "active", null, Set.of(), null, null
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
