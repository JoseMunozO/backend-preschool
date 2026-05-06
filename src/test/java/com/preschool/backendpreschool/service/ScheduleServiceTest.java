package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ScheduleSlotRequest;
import com.preschool.backendpreschool.dto.ScheduleSlotResponse;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentRequest;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.ScheduleSlot;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.StaffGroupRole;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.ScheduleSlotRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void createScheduleSlotStoresActivityWithGroupAndPrimaryStaff() {
        ClassGroup classGroup = buildClassGroup();
        Staff staff = buildStaff();

        when(classGroupRepository.findById(1L)).thenReturn(Optional.of(classGroup));
        when(staffRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(invocation -> {
            ScheduleSlot slot = invocation.getArgument(0);
            slot.setScheduleSlotId(10L);
            return slot;
        });

        ScheduleSlotResponse response = scheduleService.createScheduleSlot(new ScheduleSlotRequest(
                1L,
                3L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(10, 30),
                "Actividad educativa",
                "Aula Azul",
                "Lectura y dibujo"
        ));

        assertThat(response.scheduleSlotId()).isEqualTo(10L);
        assertThat(response.groupId()).isEqualTo(1L);
        assertThat(response.groupName()).isEqualTo("Aula Azul");
        assertThat(response.primaryStaffId()).isEqualTo(3L);
        assertThat(response.primaryStaffName()).isEqualTo("Ana Teacher");
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.activityTitle()).isEqualTo("Actividad educativa");
    }

    @Test
    void createScheduleSlotRejectsEndTimeBeforeStartTime() {
        ScheduleSlotRequest request = new ScheduleSlotRequest(
                1L,
                null,
                DayOfWeek.TUESDAY,
                LocalTime.of(11, 0),
                LocalTime.of(10, 0),
                "Horario invalido",
                null,
                null
        );

        assertThatThrownBy(() -> scheduleService.createScheduleSlot(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La hora de fin debe ser posterior a la hora de inicio");

        verify(classGroupRepository, never()).findById(any(Long.class));
        verify(scheduleSlotRepository, never()).save(any(ScheduleSlot.class));
    }

    @Test
    void assignStaffToGroupCreatesTeacherAssignmentByDefault() {
        ClassGroup classGroup = buildClassGroup();
        Staff staff = buildStaff();

        when(classGroupRepository.findById(1L)).thenReturn(Optional.of(classGroup));
        when(staffRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.save(any(StaffGroupAssignment.class))).thenAnswer(invocation -> {
            StaffGroupAssignment assignment = invocation.getArgument(0);
            assignment.setStaffGroupAssignmentId(15L);
            return assignment;
        });

        StaffGroupAssignmentResponse response = scheduleService.assignStaffToGroup(new StaffGroupAssignmentRequest(
                3L,
                1L,
                null,
                null,
                LocalDate.of(2026, 8, 15),
                null
        ));

        assertThat(response.staffGroupAssignmentId()).isEqualTo(15L);
        assertThat(response.staffId()).isEqualTo(3L);
        assertThat(response.staffName()).isEqualTo("Ana Teacher");
        assertThat(response.groupId()).isEqualTo(1L);
        assertThat(response.roleInGroup()).isEqualTo(StaffGroupRole.TEACHER);
        assertThat(response.primary()).isFalse();
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void assignStaffToGroupRejectsEndDateBeforeStartDate() {
        StaffGroupAssignmentRequest request = new StaffGroupAssignmentRequest(
                3L,
                1L,
                StaffGroupRole.ASSISTANT,
                true,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 10)
        );

        assertThatThrownBy(() -> scheduleService.assignStaffToGroup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La fecha de fin no puede ser anterior a la fecha de inicio");

        verify(staffRepository, never()).findById(any(Long.class));
        verify(staffGroupAssignmentRepository, never()).save(any(StaffGroupAssignment.class));
    }

    private ClassGroup buildClassGroup() {
        return ClassGroup.builder()
                .groupId(1L)
                .name("Aula Azul")
                .schoolYear("2026")
                .levelName("Preescolar")
                .build();
    }

    private Staff buildStaff() {
        return Staff.builder()
                .staffId(3L)
                .firstName("Ana")
                .lastName("Teacher")
                .positionTitle("Teacher")
                .staffType("teacher")
                .status("active")
                .build();
    }
}
