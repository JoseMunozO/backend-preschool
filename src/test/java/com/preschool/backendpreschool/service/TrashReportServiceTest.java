package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.TrashEntryResponse;
import com.preschool.backendpreschool.model.Material;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.TrashEntityType;
import com.preschool.backendpreschool.repository.MaterialRepository;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.ScheduleSlotRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashReportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private TrashReportService trashReportService;

    @Test
    void getTrashMergesEveryEntityIncludingArchivedParentsAndDeactivatedStaff() {
        LocalDateTime studentDeletedAt = LocalDateTime.of(2026, 8, 20, 10, 0);
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz")
                .deletedAt(studentDeletedAt).build();

        LocalDateTime materialDeletedAt = LocalDateTime.of(2026, 8, 21, 10, 0);
        Material material = Material.builder().materialId(2L).name("Toallas").deletedAt(materialDeletedAt).build();

        LocalDateTime parentDeletedAt = LocalDateTime.of(2026, 8, 22, 10, 0);
        Parent softDeletedParent = Parent.builder().parentId(3L).firstName("Luis").lastName("Perez")
                .deletedAt(parentDeletedAt).build();

        LocalDateTime parentArchivedAt = LocalDateTime.of(2020, 1, 1, 10, 0);
        Parent archivedParent = Parent.builder().parentId(4L).firstName("Marta").lastName("Ruiz")
                .archivedAt(parentArchivedAt).build();

        LocalDateTime staffDeletedAt = LocalDateTime.of(2026, 8, 23, 10, 0);
        Staff staff = Staff.builder().staffId(5L).firstName("Carlos").lastName("Gomez").deletedAt(staffDeletedAt).build();

        when(studentRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(student));
        when(materialRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(material));
        when(parentRepository.findAllByDeletedAtIsNotNullAndArchivedAtIsNull()).thenReturn(List.of(softDeletedParent));
        when(parentRepository.findAllByArchivedAtIsNotNull()).thenReturn(List.of(archivedParent));
        when(scheduleSlotRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of());
        when(staffRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(staff));

        List<TrashEntryResponse> trash = trashReportService.getTrash();

        assertThat(trash).hasSize(5);

        TrashEntryResponse studentEntry = findEntry(trash, TrashEntityType.STUDENT);
        assertThat(studentEntry.entityId()).isEqualTo(1L);
        assertThat(studentEntry.label()).isEqualTo("Ana Diaz");
        assertThat(studentEntry.purgeDeadline()).isEqualTo(studentDeletedAt.plusDays(7));

        TrashEntryResponse parentEntry = findEntry(trash, TrashEntityType.PARENT);
        assertThat(parentEntry.purgeDeadline()).isEqualTo(parentDeletedAt.plusDays(7));

        TrashEntryResponse archivedParentEntry = findEntry(trash, TrashEntityType.PARENT_ARCHIVED);
        assertThat(archivedParentEntry.entityId()).isEqualTo(4L);
        assertThat(archivedParentEntry.purgeDeadline()).isEqualTo(parentArchivedAt.plusYears(6));

        TrashEntryResponse staffEntry = findEntry(trash, TrashEntityType.STAFF);
        assertThat(staffEntry.entityId()).isEqualTo(5L);
        assertThat(staffEntry.purgeDeadline()).isNull();
    }

    private TrashEntryResponse findEntry(List<TrashEntryResponse> trash, TrashEntityType type) {
        return trash.stream()
                .filter(entry -> entry.entityType() == type)
                .findFirst()
                .orElseThrow();
    }
}
