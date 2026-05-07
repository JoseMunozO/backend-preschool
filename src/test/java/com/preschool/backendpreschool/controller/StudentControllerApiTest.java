package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.StudentResponse;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.StudentConsentService;
import com.preschool.backendpreschool.service.StudentGuardianService;
import com.preschool.backendpreschool.service.StudentNoteService;
import com.preschool.backendpreschool.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(StudentController.class)
@Import({SecurityConfig.class, StudentControllerApiTest.SecurityTestConfig.class})
class StudentControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private StudentGuardianService studentGuardianService;

    @MockitoBean
    private StudentNoteService studentNoteService;

    @MockitoBean
    private StudentConsentService studentConsentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListAndGetStudents() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of(student()));
        when(studentService.getStudentById(1L)).thenReturn(student());

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ana"));

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1));

        verify(studentService).getStudentById(1L);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadCreateAndDeleteStudentsUnderCurrentSecurityRules() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of(student()));
        when(studentService.createStudent(org.mockito.ArgumentMatchers.any())).thenReturn(student());

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validStudentRequest()))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotAccessStudentAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    private StudentResponse student() {
        return new StudentResponse(
                1L,
                "STU-001",
                "Ana",
                "Diaz",
                null,
                LocalDate.of(2021, 5, 10),
                2L,
                "Grupo A",
                StudentStatus.active,
                LocalDate.of(2024, 8, 1),
                null,
                "Sin notas",
                "Ninguna",
                "Observacion",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String validStudentRequest() {
        return """
                {
                  "studentCode": "STU-001",
                  "firstName": "Ana",
                  "lastName": "Diaz",
                  "birthDate": "2021-05-10",
                  "groupId": 2,
                  "status": "active",
                  "enrollmentDate": "2024-08-01"
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
