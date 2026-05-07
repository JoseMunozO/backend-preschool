package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.DashboardAdminSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardFinanceAreaSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardTeacherSummaryResponse;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Material;
import com.preschool.backendpreschool.model.MaterialStatus;
import com.preschool.backendpreschool.model.ParentStatus;
import com.preschool.backendpreschool.model.Payment;
import com.preschool.backendpreschool.model.ScheduleSlot;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.MaterialRepository;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.PaymentRepository;
import com.preschool.backendpreschool.repository.ScheduleSlotRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentChargeRepository studentChargeRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void summariesReturnRoleSpecificDashboardData() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        Student student = Student.builder()
                .studentId(1L)
                .firstName("Ana")
                .lastName("Diaz")
                .birthDate(today.plusDays(10).minusYears(5))
                .status(StudentStatus.active)
                .build();
        ChargeType chargeType = ChargeType.builder()
                .chargeTypeId(1L)
                .code("MONTHLY")
                .name("Mensualidad")
                .build();
        StudentCharge pendingCharge = buildCharge(10L, student, chargeType, StudentChargeStatus.PENDING, "100.00");
        StudentCharge partialCharge = buildCharge(11L, student, chargeType, StudentChargeStatus.PARTIALLY_PAID, "80.00");
        StudentCharge overdueCharge = buildCharge(12L, student, chargeType, StudentChargeStatus.OVERDUE, "50.00");
        Material lowStockMaterial = Material.builder()
                .materialId(7L)
                .sku("MAT-LOW")
                .name("Papel")
                .category("limpieza")
                .quantityOnHand(2)
                .minimumQuantity(5)
                .status(MaterialStatus.ACTIVE)
                .build();
        ScheduleSlot scheduleSlot = ScheduleSlot.builder()
                .scheduleSlotId(20L)
                .classGroup(ClassGroup.builder().groupId(3L).name("Grupo A").build())
                .primaryStaff(Staff.builder().staffId(4L).firstName("Luis").lastName("Rojas").build())
                .dayOfWeek(today.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .activityTitle("Lectura")
                .roomName("Aula 1")
                .build();
        Payment payment = Payment.builder()
                .paymentId(30L)
                .paymentDate(today)
                .totalAmount(new BigDecimal("120.00"))
                .build();

        when(studentRepository.count()).thenReturn(4L);
        when(studentRepository.countByStatus(StudentStatus.active)).thenReturn(3L);
        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(parentRepository.count()).thenReturn(5L);
        when(parentRepository.countByStatus(ParentStatus.ACTIVE)).thenReturn(4L);
        when(materialRepository.countByStatus(MaterialStatus.ACTIVE)).thenReturn(8L);
        when(materialRepository.findActiveLowStock()).thenReturn(List.of(lowStockMaterial));
        when(scheduleSlotRepository.findByDayOfWeekOrderByStartTimeAsc(today.getDayOfWeek())).thenReturn(List.of(scheduleSlot));
        when(studentChargeRepository.findAll()).thenReturn(List.of(pendingCharge, partialCharge, overdueCharge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(BigDecimal.ZERO);
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(11L)).thenReturn(new BigDecimal("30.00"));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(12L)).thenReturn(new BigDecimal("10.00"));
        when(paymentRepository.findByPaymentDateBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth())).thenReturn(List.of(payment));

        DashboardTeacherSummaryResponse teacherSummary = dashboardService.getTeacherSummary();
        DashboardAdminSummaryResponse adminSummary = dashboardService.getAdminSummary();
        DashboardFinanceAreaSummaryResponse financeSummary = dashboardService.getFinanceSummary();

        assertThat(teacherSummary.activeStudents()).isEqualTo(3);
        assertThat(teacherSummary.todayScheduleSlots()).isEqualTo(1);
        assertThat(teacherSummary.lowStockMaterials()).isEqualTo(1);
        assertThat(teacherSummary.todaySchedule()).singleElement()
                .satisfies(slot -> {
                    assertThat(slot.scheduleSlotId()).isEqualTo(20L);
                    assertThat(slot.groupName()).isEqualTo("Grupo A");
                    assertThat(slot.primaryStaffName()).isEqualTo("Luis Rojas");
                    assertThat(slot.dayOfWeek()).isEqualTo(DayOfWeek.from(today));
                });
        assertThat(teacherSummary.upcomingBirthdays()).singleElement()
                .satisfies(birthday -> {
                    assertThat(birthday.studentId()).isEqualTo(1L);
                    assertThat(birthday.studentName()).isEqualTo("Ana Diaz");
                    assertThat(birthday.daysUntilBirthday()).isEqualTo(10);
                });

        assertThat(adminSummary.totalStudents()).isEqualTo(4);
        assertThat(adminSummary.activeStudents()).isEqualTo(3);
        assertThat(adminSummary.totalParents()).isEqualTo(5);
        assertThat(adminSummary.activeParents()).isEqualTo(4);
        assertThat(adminSummary.totalMaterials()).isEqualTo(8);
        assertThat(adminSummary.lowStockMaterials()).isEqualTo(1);
        assertThat(adminSummary.todayScheduleSlots()).isEqualTo(1);
        assertThat(adminSummary.lowStockMaterialAlerts()).singleElement()
                .satisfies(material -> {
                    assertThat(material.materialId()).isEqualTo(7L);
                    assertThat(material.shortage()).isEqualTo(3);
                });
        assertThat(adminSummary.todaySchedule()).singleElement()
                .satisfies(slot -> {
                    assertThat(slot.scheduleSlotId()).isEqualTo(20L);
                    assertThat(slot.groupName()).isEqualTo("Grupo A");
                    assertThat(slot.primaryStaffName()).isEqualTo("Luis Rojas");
                    assertThat(slot.dayOfWeek()).isEqualTo(DayOfWeek.from(today));
                });
        assertThat(adminSummary.upcomingBirthdays()).singleElement()
                .satisfies(birthday -> {
                    assertThat(birthday.studentId()).isEqualTo(1L);
                    assertThat(birthday.studentName()).isEqualTo("Ana Diaz");
                    assertThat(birthday.daysUntilBirthday()).isEqualTo(10);
                });

        assertThat(financeSummary.pendingCharges()).isEqualTo(2);
        assertThat(financeSummary.overdueCharges()).isEqualTo(1);
        assertThat(financeSummary.pendingBalance()).isEqualByComparingTo("150.00");
        assertThat(financeSummary.overdueBalance()).isEqualByComparingTo("40.00");
        assertThat(financeSummary.monthPaymentsReceived()).isEqualByComparingTo("120.00");
    }

    private StudentCharge buildCharge(
            Long studentChargeId,
            Student student,
            ChargeType chargeType,
            StudentChargeStatus status,
            String amountDue
    ) {
        return StudentCharge.builder()
                .studentChargeId(studentChargeId)
                .student(student)
                .chargeType(chargeType)
                .dueDate(LocalDate.now())
                .amountDue(new BigDecimal(amountDue))
                .status(status)
                .build();
    }
}
