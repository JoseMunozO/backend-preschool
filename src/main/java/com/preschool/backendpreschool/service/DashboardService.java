package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.DashboardAdminSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardBirthdayResponse;
import com.preschool.backendpreschool.dto.DashboardFinanceAreaSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardMaterialAlertResponse;
import com.preschool.backendpreschool.dto.DashboardScheduleItemResponse;
import com.preschool.backendpreschool.dto.DashboardTeacherSummaryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int ALERT_LIMIT = 5;
    private static final int BIRTHDAY_LIMIT = 5;
    private static final int BIRTHDAY_LOOKAHEAD_DAYS = 30;

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentChargeRepository studentChargeRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentRepository paymentRepository;
    private final MaterialRepository materialRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    public DashboardTeacherSummaryResponse getTeacherSummary() {
        LocalDate today = LocalDate.now();
        List<Material> allLowStockMaterials = materialRepository.findActiveLowStock();
        List<ScheduleSlot> todaySchedule = todaySchedule(today);

        return new DashboardTeacherSummaryResponse(
                today,
                studentRepository.countByStatus(StudentStatus.active),
                todaySchedule.size(),
                allLowStockMaterials.size(),
                todaySchedule.stream().map(this::toScheduleItem).toList(),
                upcomingBirthdays(today),
                lowStockMaterialAlerts(allLowStockMaterials)
        );
    }

    public DashboardAdminSummaryResponse getAdminSummary() {
        LocalDate today = LocalDate.now();
        List<Material> allLowStockMaterials = materialRepository.findActiveLowStock();
        List<ScheduleSlot> todaySchedule = todaySchedule(today);

        return new DashboardAdminSummaryResponse(
                today,
                studentRepository.count(),
                studentRepository.countByStatus(StudentStatus.active),
                parentRepository.count(),
                parentRepository.countByStatus(ParentStatus.ACTIVE),
                materialRepository.countByStatus(MaterialStatus.ACTIVE),
                allLowStockMaterials.size(),
                todaySchedule.size(),
                lowStockMaterialAlerts(allLowStockMaterials),
                todaySchedule.stream().map(this::toScheduleItem).toList(),
                upcomingBirthdays(today)
        );
    }

    public DashboardFinanceAreaSummaryResponse getFinanceSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        List<StudentCharge> charges = studentChargeRepository.findAll();

        return new DashboardFinanceAreaSummaryResponse(
                today,
                currentMonth,
                countChargesByStatus(charges, StudentChargeStatus.PENDING, StudentChargeStatus.PARTIALLY_PAID),
                countChargesByStatus(charges, StudentChargeStatus.OVERDUE),
                totalBalance(charges, StudentChargeStatus.PENDING, StudentChargeStatus.PARTIALLY_PAID),
                totalBalance(charges, StudentChargeStatus.OVERDUE),
                totalPaymentsReceived(currentMonth)
        );
    }

    private List<ScheduleSlot> todaySchedule(LocalDate today) {
        return scheduleSlotRepository.findByDayOfWeekOrderByStartTimeAsc(today.getDayOfWeek());
    }

    private List<DashboardMaterialAlertResponse> lowStockMaterialAlerts(List<Material> materials) {
        return materials.stream()
                .sorted(Comparator
                        .comparingInt(this::shortage).reversed()
                        .thenComparing(Material::getName))
                .limit(ALERT_LIMIT)
                .map(this::toMaterialAlert)
                .toList();
    }

    private long countChargesByStatus(List<StudentCharge> charges, StudentChargeStatus... statuses) {
        return charges.stream()
                .filter(charge -> hasStatus(charge, statuses))
                .count();
    }

    private BigDecimal totalBalance(List<StudentCharge> charges, StudentChargeStatus... statuses) {
        return charges.stream()
                .filter(charge -> hasStatus(charge, statuses))
                .map(this::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasStatus(StudentCharge charge, StudentChargeStatus... statuses) {
        for (StudentChargeStatus status : statuses) {
            if (charge.getStatus() == status) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal balance(StudentCharge charge) {
        BigDecimal paid = paymentAllocationRepository.sumAllocatedByStudentChargeId(charge.getStudentChargeId());
        return charge.getAmountDue().subtract(paid).max(BigDecimal.ZERO);
    }

    private BigDecimal totalPaymentsReceived(YearMonth month) {
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();

        return paymentRepository.findByPaymentDateBetween(firstDay, lastDay)
                .stream()
                .map(Payment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DashboardMaterialAlertResponse toMaterialAlert(Material material) {
        return new DashboardMaterialAlertResponse(
                material.getMaterialId(),
                material.getSku(),
                material.getName(),
                material.getCategory(),
                material.getQuantityOnHand(),
                material.getMinimumQuantity(),
                shortage(material)
        );
    }

    private DashboardScheduleItemResponse toScheduleItem(ScheduleSlot slot) {
        ClassGroup classGroup = slot.getClassGroup();
        Staff primaryStaff = slot.getPrimaryStaff();

        return new DashboardScheduleItemResponse(
                slot.getScheduleSlotId(),
                classGroup.getGroupId(),
                classGroup.getName(),
                primaryStaff != null ? primaryStaff.getStaffId() : null,
                primaryStaff != null ? fullName(primaryStaff) : null,
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getActivityTitle(),
                slot.getRoomName()
        );
    }

    private List<DashboardBirthdayResponse> upcomingBirthdays(LocalDate today) {
        return studentRepository.findAll()
                .stream()
                .filter(student -> student.getStatus() == StudentStatus.active)
                .map(student -> toBirthdayResponse(student, today))
                .filter(birthday -> birthday.daysUntilBirthday() <= BIRTHDAY_LOOKAHEAD_DAYS)
                .sorted(Comparator.comparingInt(DashboardBirthdayResponse::daysUntilBirthday))
                .limit(BIRTHDAY_LIMIT)
                .toList();
    }

    private DashboardBirthdayResponse toBirthdayResponse(Student student, LocalDate today) {
        LocalDate nextBirthday = student.getBirthDate().withYear(today.getYear());
        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }

        return new DashboardBirthdayResponse(
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getBirthDate(),
                nextBirthday,
                (int) ChronoUnit.DAYS.between(today, nextBirthday)
        );
    }

    private int shortage(Material material) {
        return Math.max(material.getMinimumQuantity() - material.getQuantityOnHand(), 0);
    }

    private String fullName(Staff staff) {
        return staff.getFirstName() + " " + staff.getLastName();
    }
}
