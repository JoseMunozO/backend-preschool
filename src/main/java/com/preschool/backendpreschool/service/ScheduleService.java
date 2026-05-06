package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ScheduleSlotRequest;
import com.preschool.backendpreschool.dto.ScheduleSlotResponse;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentRequest;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.ScheduleSlot;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.StaffGroupRole;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.ScheduleSlotRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleSlotRepository scheduleSlotRepository;
    private final StaffGroupAssignmentRepository staffGroupAssignmentRepository;
    private final ClassGroupRepository classGroupRepository;
    private final StaffRepository staffRepository;

    public List<ScheduleSlotResponse> getScheduleSlots(Long groupId, DayOfWeek dayOfWeek) {
        if (groupId != null && !classGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo no encontrado");
        }

        List<ScheduleSlot> slots;
        if (groupId != null && dayOfWeek != null) {
            slots = scheduleSlotRepository.findByClassGroupGroupIdAndDayOfWeekOrderByStartTimeAsc(groupId, dayOfWeek);
        } else if (groupId != null) {
            slots = scheduleSlotRepository.findByClassGroupGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId);
        } else if (dayOfWeek != null) {
            slots = scheduleSlotRepository.findByDayOfWeekOrderByStartTimeAsc(dayOfWeek);
        } else {
            slots = scheduleSlotRepository.findAll()
                    .stream()
                    .sorted((left, right) -> {
                        int dayComparison = left.getDayOfWeek().compareTo(right.getDayOfWeek());
                        return dayComparison != 0 ? dayComparison : left.getStartTime().compareTo(right.getStartTime());
                    })
                    .toList();
        }

        return slots.stream()
                .map(this::toScheduleSlotResponse)
                .toList();
    }

    public ScheduleSlotResponse getScheduleSlotById(Long scheduleSlotId) {
        return toScheduleSlotResponse(findScheduleSlot(scheduleSlotId));
    }

    @Transactional
    public ScheduleSlotResponse createScheduleSlot(ScheduleSlotRequest request) {
        validateScheduleSlotTimes(request);

        ScheduleSlot slot = ScheduleSlot.builder()
                .classGroup(findClassGroup(request.groupId()))
                .primaryStaff(findOptionalStaff(request.primaryStaffId()))
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .activityTitle(request.activityTitle().trim())
                .roomName(trimToNull(request.roomName()))
                .notes(trimToNull(request.notes()))
                .build();

        return toScheduleSlotResponse(scheduleSlotRepository.save(slot));
    }

    @Transactional
    public ScheduleSlotResponse updateScheduleSlot(Long scheduleSlotId, ScheduleSlotRequest request) {
        validateScheduleSlotTimes(request);

        ScheduleSlot slot = findScheduleSlot(scheduleSlotId);
        slot.setClassGroup(findClassGroup(request.groupId()));
        slot.setPrimaryStaff(findOptionalStaff(request.primaryStaffId()));
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setActivityTitle(request.activityTitle().trim());
        slot.setRoomName(trimToNull(request.roomName()));
        slot.setNotes(trimToNull(request.notes()));

        return toScheduleSlotResponse(scheduleSlotRepository.save(slot));
    }

    @Transactional
    public ScheduleSlotResponse assignPrimaryStaff(Long scheduleSlotId, Long staffId) {
        ScheduleSlot slot = findScheduleSlot(scheduleSlotId);
        slot.setPrimaryStaff(findStaff(staffId));

        return toScheduleSlotResponse(scheduleSlotRepository.save(slot));
    }

    public List<StaffGroupAssignmentResponse> getStaffGroupAssignments(Long groupId, Long staffId) {
        if (groupId != null && !classGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo no encontrado");
        }
        if (staffId != null && !staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Personal no encontrado");
        }

        List<StaffGroupAssignment> assignments;
        if (groupId != null) {
            assignments = staffGroupAssignmentRepository.findByClassGroupGroupIdOrderByStartDateDesc(groupId);
        } else if (staffId != null) {
            assignments = staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(staffId);
        } else {
            assignments = staffGroupAssignmentRepository.findAll();
        }

        return assignments.stream()
                .map(this::toStaffGroupAssignmentResponse)
                .toList();
    }

    @Transactional
    public StaffGroupAssignmentResponse assignStaffToGroup(StaffGroupAssignmentRequest request) {
        validateAssignmentDates(request);

        StaffGroupAssignment assignment = StaffGroupAssignment.builder()
                .staff(findStaff(request.staffId()))
                .classGroup(findClassGroup(request.groupId()))
                .roleInGroup(request.roleInGroup() != null ? request.roleInGroup() : StaffGroupRole.TEACHER)
                .primary(request.primary() != null ? request.primary() : false)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        return toStaffGroupAssignmentResponse(staffGroupAssignmentRepository.save(assignment));
    }

    private void validateScheduleSlotTimes(ScheduleSlotRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private void validateAssignmentDates(StaffGroupAssignmentRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private ScheduleSlot findScheduleSlot(Long scheduleSlotId) {
        return scheduleSlotRepository.findById(scheduleSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad de horario no encontrada"));
    }

    private ClassGroup findClassGroup(Long groupId) {
        return classGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
    }

    private Staff findStaff(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));
    }

    private Staff findOptionalStaff(Long staffId) {
        return staffId == null ? null : findStaff(staffId);
    }

    private ScheduleSlotResponse toScheduleSlotResponse(ScheduleSlot slot) {
        ClassGroup classGroup = slot.getClassGroup();
        Staff primaryStaff = slot.getPrimaryStaff();

        return new ScheduleSlotResponse(
                slot.getScheduleSlotId(),
                classGroup.getGroupId(),
                classGroup.getName(),
                primaryStaff != null ? primaryStaff.getStaffId() : null,
                primaryStaff != null ? fullName(primaryStaff) : null,
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getActivityTitle(),
                slot.getRoomName(),
                slot.getNotes(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }

    private StaffGroupAssignmentResponse toStaffGroupAssignmentResponse(StaffGroupAssignment assignment) {
        Staff staff = assignment.getStaff();
        ClassGroup classGroup = assignment.getClassGroup();

        return new StaffGroupAssignmentResponse(
                assignment.getStaffGroupAssignmentId(),
                staff.getStaffId(),
                fullName(staff),
                classGroup.getGroupId(),
                classGroup.getName(),
                assignment.getRoleInGroup(),
                assignment.getPrimary(),
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.getCreatedAt()
        );
    }

    private String fullName(Staff staff) {
        return staff.getFirstName() + " " + staff.getLastName();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
