package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.ChargeTypeResponse;
import com.preschool.backendpreschool.dto.PaymentMonthlyReportResponse;
import com.preschool.backendpreschool.dto.PaymentResponse;
import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.PaymentMethod;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.MonthlyChargeGenerationService;
import com.preschool.backendpreschool.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, PaymentControllerApiTest.SecurityTestConfig.class})
class PaymentControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private MonthlyChargeGenerationService monthlyChargeGenerationService;

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeCanListChargesWithFilters() throws Exception {
        when(paymentService.getCharges(1L, StudentChargeStatus.PENDING, YearMonth.of(2026, 5)))
                .thenReturn(List.of(charge()));

        mockMvc.perform(get("/api/payments/charges")
                        .param("studentId", "1")
                        .param("status", "PENDING")
                        .param("month", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentChargeId").value(10))
                .andExpect(jsonPath("$[0].balance").value(100.0));

        verify(paymentService).getCharges(1L, StudentChargeStatus.PENDING, YearMonth.of(2026, 5));
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    void parentCanReadOwnChargesButCannotListAllPayments() throws Exception {
        when(paymentService.getCurrentParentCharges("parent@example.com")).thenReturn(List.of(charge()));

        mockMvc.perform(get("/api/payments/me/charges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentChargeId").value(10));
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isForbidden());

        verify(paymentService).getCurrentParentCharges("parent@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListChargeTypesAndPayments() throws Exception {
        when(paymentService.getChargeTypes(true)).thenReturn(List.of(chargeType()));
        when(paymentService.getPayments(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(payment()));

        mockMvc.perform(get("/api/payments/charge-types").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MONTHLY"));
        mockMvc.perform(get("/api/payments")
                        .param("parentId", "1")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(20));

        verify(paymentService).getPayments(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeCanGetMonthlyReport() throws Exception {
        when(paymentService.getMonthlyReport(YearMonth.of(2026, 6))).thenReturn(monthlyReport());

        mockMvc.perform(get("/api/payments/reports/monthly").param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.pendingBalance").value(700.0))
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.overdueBalance").value(500.0))
                .andExpect(jsonPath("$.paymentsReceived").value(1070.0));

        verify(paymentService).getMonthlyReport(YearMonth.of(2026, 6));
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeCanUpdateChargeToCancelIt() throws Exception {
        when(paymentService.updateCharge(eq(10L), any())).thenReturn(charge());

        mockMvc.perform(put("/api/payments/charges/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": 1,
                                  "chargeTypeId": 1,
                                  "dueDate": "2026-05-31",
                                  "amountDue": 100.00,
                                  "status": "CANCELLED",
                                  "description": "Cancelado por cambio de plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentChargeId").value(10));

        verify(paymentService).updateCharge(eq(10L), any());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeCanTriggerMonthlyChargeGeneration() throws Exception {
        when(monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 6))).thenReturn(List.of(charge()));

        mockMvc.perform(post("/api/payments/generate-monthly-charges").param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentChargeId").value(10));

        verify(monthlyChargeGenerationService).generateMonthlyCharges(YearMonth.of(2026, 6));
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    void parentCannotAccessMonthlyReport() throws Exception {
        mockMvc.perform(get("/api/payments/reports/monthly"))
                .andExpect(status().isForbidden());
    }

    private PaymentMonthlyReportResponse monthlyReport() {
        return new PaymentMonthlyReportResponse(
                YearMonth.of(2026, 6),
                1,
                new BigDecimal("700.00"),
                List.of(charge()),
                1,
                new BigDecimal("500.00"),
                List.of(charge()),
                new BigDecimal("1070.00")
        );
    }

    private ChargeTypeResponse chargeType() {
        return new ChargeTypeResponse(
                1L,
                "MONTHLY",
                "Monthly fee",
                ChargeRecurrenceType.MONTHLY,
                new BigDecimal("1000.00"),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private StudentChargeResponse charge() {
        return new StudentChargeResponse(
                10L,
                1L,
                "Ana Diaz",
                1L,
                "MONTHLY",
                "Monthly fee",
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                StudentChargeStatus.PENDING,
                "May",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private PaymentResponse payment() {
        return new PaymentResponse(
                20L,
                1L,
                "Ana Parent",
                null,
                null,
                LocalDate.of(2026, 5, 7),
                new BigDecimal("100.00"),
                PaymentMethod.CARD,
                "REF-1",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of()
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
