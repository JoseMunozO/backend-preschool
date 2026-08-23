package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.StudentAttendanceResponse;
import com.preschool.backendpreschool.model.StudentAttendanceStatus;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.StudentAttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentAttendanceController.class)
@Import({SecurityConfig.class, StudentAttendanceControllerApiTest.SecurityTestConfig.class})
class StudentAttendanceControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentAttendanceService studentAttendanceService;

    @Test
    @WithMockUser(username = "teacher@school.com", roles = "TEACHER")
    void teacherCanReadAndSaveAttendance() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 24);
        StudentAttendanceResponse response = new StudentAttendanceResponse(
                1L, 10L, "Ana Diaz", date, StudentAttendanceStatus.PRESENT, null, 2L, "teacher@school.com", null, null
        );
        when(studentAttendanceService.getAttendance(5L, date, "teacher@school.com")).thenReturn(List.of(response));
        when(studentAttendanceService.saveAttendance(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("teacher@school.com")))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/attendance").param("groupId", "5").param("date", "2026-08-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(10))
                .andExpect(jsonPath("$[0].status").value("PRESENT"));

        mockMvc.perform(post("/api/attendance")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupId": 5,
                                  "date": "2026-08-24",
                                  "records": [
                                    {"studentId": 10, "status": "PRESENT", "notes": null}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(10));

        verify(studentAttendanceService).getAttendance(5L, date, "teacher@school.com");
    }

    @Test
    @WithMockUser(username = "admin@school.com", roles = "ADMIN")
    void adminCanReadStudentAttendanceHistory() throws Exception {
        LocalDate date = LocalDate.now();
        StudentAttendanceResponse response = new StudentAttendanceResponse(
                1L, 10L, "Ana Diaz", date, StudentAttendanceStatus.PRESENT, null, 2L, "admin@school.com", null, null
        );
        when(studentAttendanceService.getStudentAttendanceHistory(10L, null, null, "admin@school.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/attendance/students/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(10));

        verify(studentAttendanceService).getStudentAttendanceHistory(10L, null, null, "admin@school.com");
    }

    @Test
    @WithMockUser(username = "parent.demo@school.com", roles = "PARENT")
    void parentCannotAccessAttendance() throws Exception {
        mockMvc.perform(get("/api/attendance").param("groupId", "5"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessAttendance() throws Exception {
        mockMvc.perform(get("/api/attendance").param("groupId", "5"))
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
