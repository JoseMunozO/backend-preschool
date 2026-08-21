package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.ScheduleSlotResponse;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentResponse;
import com.preschool.backendpreschool.model.StaffGroupRole;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@Import({SecurityConfig.class, ScheduleControllerApiTest.SecurityTestConfig.class})
class ScheduleControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadAndCreateSchedulesUnderCurrentSecurityRules() throws Exception {
        when(scheduleService.getScheduleSlots(1L, DayOfWeek.MONDAY, null)).thenReturn(List.of(slot()));
        when(scheduleService.createScheduleSlot(any())).thenReturn(slot());

        mockMvc.perform(get("/api/schedules")
                        .param("groupId", "1")
                        .param("dayOfWeek", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleSlotId").value(1))
                .andExpect(jsonPath("$[0].activityTitle").value("Circle time"));
        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScheduleRequest()))
                .andExpect(status().isCreated());

        verify(scheduleService).getScheduleSlots(1L, DayOfWeek.MONDAY, null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateScheduleAndListStaffAssignments() throws Exception {
        when(scheduleService.createScheduleSlot(any())).thenReturn(slot());
        when(scheduleService.getStaffGroupAssignments(1L, 2L)).thenReturn(List.of(assignment()));

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScheduleRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleSlotId").value(1));
        mockMvc.perform(get("/api/schedules/staff-assignments")
                        .param("groupId", "1")
                        .param("staffId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffGroupAssignmentId").value(5));

        verify(scheduleService).getStaffGroupAssignments(1L, 2L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteAndRestoreScheduleSlot() throws Exception {
        when(scheduleService.restoreScheduleSlot(1L)).thenReturn(slot());

        mockMvc.perform(delete("/api/schedules/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/schedules/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleSlotId").value(1));

        verify(scheduleService).deleteScheduleSlot(1L);
        verify(scheduleService).restoreScheduleSlot(1L);
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotAccessSchedules() throws Exception {
        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isForbidden());
    }

    private ScheduleSlotResponse slot() {
        return new ScheduleSlotResponse(
                1L,
                1L,
                "Group A",
                2L,
                "Luis Rojas",
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Circle time",
                "Room 1",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private StaffGroupAssignmentResponse assignment() {
        return new StaffGroupAssignmentResponse(
                5L,
                2L,
                "Luis Rojas",
                1L,
                "Group A",
                StaffGroupRole.TEACHER,
                true,
                LocalDate.of(2026, 1, 1),
                null,
                LocalDateTime.now()
        );
    }

    private String validScheduleRequest() {
        return """
                {
                  "groupId": 1,
                  "primaryStaffId": 2,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "10:00:00",
                  "activityTitle": "Circle time",
                  "roomName": "Room 1"
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
