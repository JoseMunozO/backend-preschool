package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.TrashEntryResponse;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Material;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.ScheduleSlot;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.TrashEntityType;
import com.preschool.backendpreschool.repository.MaterialRepository;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.ScheduleSlotRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bird's-eye view across every soft-deletable entity's trash/archive state - each entity already
 * has its own dedicated restore UI, this just answers "what's about to be lost, across all of them".
 */
@Service
@RequiredArgsConstructor
public class TrashReportService {

    private final StudentRepository studentRepository;
    private final MaterialRepository materialRepository;
    private final ParentRepository parentRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final StaffRepository staffRepository;

    public List<TrashEntryResponse> getTrash() {
        List<TrashEntryResponse> entries = new ArrayList<>();

        for (Student student : studentRepository.findAllByDeletedAtIsNotNull()) {
            entries.add(new TrashEntryResponse(
                    student.getStudentId(),
                    TrashEntityType.STUDENT,
                    student.getFirstName() + " " + student.getLastName(),
                    student.getDeletedAt(),
                    student.getDeletedAt().plusDays(StudentService.RESTORE_GRACE_PERIOD_DAYS)
            ));
        }

        for (Material material : materialRepository.findAllByDeletedAtIsNotNull()) {
            entries.add(new TrashEntryResponse(
                    material.getMaterialId(),
                    TrashEntityType.MATERIAL,
                    material.getName(),
                    material.getDeletedAt(),
                    material.getDeletedAt().plusDays(MaterialService.RESTORE_GRACE_PERIOD_DAYS)
            ));
        }

        for (Parent parent : parentRepository.findAllByDeletedAtIsNotNullAndArchivedAtIsNull()) {
            entries.add(new TrashEntryResponse(
                    parent.getParentId(),
                    TrashEntityType.PARENT,
                    parent.getFirstName() + " " + parent.getLastName(),
                    parent.getDeletedAt(),
                    parent.getDeletedAt().plusDays(ParentService.RESTORE_GRACE_PERIOD_DAYS)
            ));
        }

        for (Parent parent : parentRepository.findAllByArchivedAtIsNotNull()) {
            entries.add(new TrashEntryResponse(
                    parent.getParentId(),
                    TrashEntityType.PARENT_ARCHIVED,
                    parent.getFirstName() + " " + parent.getLastName(),
                    parent.getArchivedAt(),
                    parent.getArchivedAt().plusYears(ParentService.ARCHIVE_RETENTION_YEARS)
            ));
        }

        for (ScheduleSlot slot : scheduleSlotRepository.findAllByDeletedAtIsNotNull()) {
            entries.add(new TrashEntryResponse(
                    slot.getScheduleSlotId(),
                    TrashEntityType.SCHEDULE_SLOT,
                    buildScheduleSlotLabel(slot),
                    slot.getDeletedAt(),
                    slot.getDeletedAt().plusDays(ScheduleService.RESTORE_GRACE_PERIOD_DAYS)
            ));
        }

        for (Staff staff : staffRepository.findAllByDeletedAtIsNotNull()) {
            entries.add(new TrashEntryResponse(
                    staff.getStaffId(),
                    TrashEntityType.STAFF,
                    staff.getFirstName() + " " + staff.getLastName(),
                    staff.getDeletedAt(),
                    null
            ));
        }

        return entries.stream()
                .sorted(Comparator.comparing(TrashEntryResponse::deletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String buildScheduleSlotLabel(ScheduleSlot slot) {
        ClassGroup group = slot.getClassGroup();
        String groupName = group != null ? group.getName() : "?";
        return slot.getActivityTitle() + " - " + groupName + " (" + slot.getDayOfWeek() + ")";
    }
}
