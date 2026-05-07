package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.DashboardAdminSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardFinanceAreaSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardMainSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardTeacherSummaryResponse;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.DashboardService;
import com.preschool.backendpreschool.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DashboardControllerSecurityTest.SecurityTestConfig.class})
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanAccessOnlyTeacherDashboard() throws Exception {
        when(dashboardService.getTeacherSummary()).thenReturn(teacherSummary());

        mockMvc.perform(get("/api/dashboard/teacher-summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/admin-summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/finance-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAllDashboardAreas() throws Exception {
        when(dashboardService.getMainSummary()).thenReturn(mainSummary());
        when(dashboardService.getTeacherSummary()).thenReturn(teacherSummary());
        when(dashboardService.getAdminSummary()).thenReturn(adminSummary());
        when(dashboardService.getFinanceSummary()).thenReturn(financeSummary());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/teacher-summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/admin-summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/finance-summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void directorCanAccessAllDashboardAreas() throws Exception {
        when(dashboardService.getMainSummary()).thenReturn(mainSummary());
        when(dashboardService.getTeacherSummary()).thenReturn(teacherSummary());
        when(dashboardService.getAdminSummary()).thenReturn(adminSummary());
        when(dashboardService.getFinanceSummary()).thenReturn(financeSummary());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/teacher-summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/admin-summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/finance-summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeCanAccessOnlyFinanceDashboard() throws Exception {
        when(dashboardService.getFinanceSummary()).thenReturn(financeSummary());

        mockMvc.perform(get("/api/dashboard/finance-summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/teacher-summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/admin-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotAccessInternalDashboards() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/teacher-summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/admin-summary"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/finance-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownDashboardRoutesAreBlocked() throws Exception {
        mockMvc.perform(get("/api/dashboard/hr-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUsersCannotAccessDashboards() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/teacher-summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/admin-summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/finance-summary"))
                .andExpect(status().isUnauthorized());
    }

    private DashboardTeacherSummaryResponse teacherSummary() {
        return new DashboardTeacherSummaryResponse(
                LocalDate.now(),
                3,
                1,
                0,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DashboardMainSummaryResponse mainSummary() {
        return new DashboardMainSummaryResponse(
                LocalDate.now(),
                adminSummary(),
                financeSummary()
        );
    }

    private DashboardAdminSummaryResponse adminSummary() {
        return new DashboardAdminSummaryResponse(
                LocalDate.now(),
                4,
                3,
                2,
                2,
                5,
                0,
                1,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DashboardFinanceAreaSummaryResponse financeSummary() {
        return new DashboardFinanceAreaSummaryResponse(
                LocalDate.now(),
                YearMonth.now(),
                2,
                1,
                ZERO,
                ZERO,
                ZERO
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
