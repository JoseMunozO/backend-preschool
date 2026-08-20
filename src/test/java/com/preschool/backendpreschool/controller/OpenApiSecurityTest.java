package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@Import({SecurityConfig.class, OpenApiSecurityTest.SecurityTestConfig.class})
class OpenApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.preschool.backendpreschool.service.StudentService studentService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.preschool.backendpreschool.service.StudentGuardianService studentGuardianService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.preschool.backendpreschool.service.StudentNoteService studentNoteService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.preschool.backendpreschool.service.StudentConsentService studentConsentService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.preschool.backendpreschool.service.StudentEmergencyContactService studentEmergencyContactService;

    @Test
    void openApiDocsRouteIsNotBlockedByAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
                });
    }

    @Test
    void swaggerUiRouteIsNotBlockedByAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
                });
    }

    @Test
    void applicationEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
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
