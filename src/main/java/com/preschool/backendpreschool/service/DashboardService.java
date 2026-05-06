package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.DashboardCountsResponse;
import com.preschool.backendpreschool.dto.DashboardBirthdayResponse;
import com.preschool.backendpreschool.dto.DashboardFinanceSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardMaterialAlertResponse;
import com.preschool.backendpreschool.dto.DashboardScheduleItemResponse;
import com.preschool.backendpreschool.dto.DashboardSummaryResponse;
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

    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        List<Material> allLowStockMaterials = materialRepository.findActiveLowStock();
        List<Material> lowStockMaterials = allLowStockMaterials
                .stream()
                .sorted(Comparator
                        .comparingInt(this::shortage).reversed()
                        .thenComparing(Material::getName))
                .limit(ALERT_LIMIT)
                .toList();

        List<ScheduleSlot> todaySchedule = scheduleSlotRepository.findByDayOfWeekOrderByStartTimeAsc(today.getDayOfWeek());
        List<StudentCharge> charges = studentChargeRepository.findAll();

        DashboardCountsResponse counts = new DashboardCountsResponse(
                studentRepository.count(),
                studentRepository.countByStatus(StudentStatus.active),
                parentRepository.count(),
                parentRepository.countByStatus(ParentStatus.ACTIVE),
                materialRepository.countByStatus(MaterialStatus.ACTIVE),
                allLowStockMaterials.size(),
                countChargesByStatus(charges, StudentChargeStatus.PENDING, StudentChargeStatus.PARTIALLY_PAID),
                countChargesByStatus(charges, StudentChargeStatus.OVERDUE),
                todaySchedule.size()
        );

        DashboardFinanceSummaryResponse finance = new DashboardFinanceSummaryResponse(
                totalBalance(charges, StudentChargeStatus.PENDING, StudentChargeStatus.PARTIALLY_PAID),
                totalBalance(charges, StudentChargeStatus.OVERDUE),
                totalPaymentsReceived(currentMonth)
        );

        return new DashboardSummaryResponse(
                today,
                currentMonth,
                counts,
                finance,
                lowStockMaterials.stream().map(this::toMaterialAlert).toList(),
                todaySchedule.stream().map(this::toScheduleItem).toList(),
                upcomingBirthdays(today)
        );
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
