package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.StudentDiscountResponse;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.StudentDiscountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentDiscountController.class)
@Import({SecurityConfig.class, StudentDiscountControllerApiTest.SecurityTestConfig.class})
class StudentDiscountControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentDiscountService studentDiscountService;

    @Test
    @WithMockUser(username = "finance@school.com", roles = "FINANCE")
    void financeCanCreateListAndDeactivateDiscount() throws Exception {
        when(studentDiscountService.getDiscounts(1L)).thenReturn(List.of(discount()));
        when(studentDiscountService.createDiscount(eq(1L), any(), eq("finance@school.com"))).thenReturn(discount());
        when(studentDiscountService.deactivateDiscount(1L, 5L)).thenReturn(discount());

        mockMvc.perform(get("/api/payments/students/1/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentDiscountId").value(5));

        mockMvc.perform(post("/api/payments/students/1/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "discountType": "PERCENTAGE",
                                  "value": 10,
                                  "reason": "Hermanos",
                                  "validFrom": "2026-08-01",
                                  "validUntil": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentDiscountId").value(5));

        mockMvc.perform(patch("/api/payments/students/1/discounts/5/deactivate"))
                .andExpect(status().isOk());

        verify(studentDiscountService).deactivateDiscount(1L, 5L);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAccessDiscounts() throws Exception {
        mockMvc.perform(get("/api/payments/students/1/discounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessDiscounts() throws Exception {
        mockMvc.perform(get("/api/payments/students/1/discounts"))
                .andExpect(status().isUnauthorized());
    }

    private StudentDiscountResponse discount() {
        return new StudentDiscountResponse(
                5L, 1L, "Ana Diaz", DiscountType.PERCENTAGE, new BigDecimal("10"), "Hermanos",
                LocalDate.of(2026, 8, 1), null, true, 2L, "finance@school.com",
                LocalDateTime.now(), LocalDateTime.now()
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
