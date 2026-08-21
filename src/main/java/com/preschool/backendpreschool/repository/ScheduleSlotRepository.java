package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    List<ScheduleSlot> findByClassGroupGroupIdOrderByDayOfWeekAscStartTimeAsc(Long groupId);

    List<ScheduleSlot> findByDayOfWeekOrderByStartTimeAsc(DayOfWeek dayOfWeek);

    List<ScheduleSlot> findByClassGroupGroupIdAndDayOfWeekOrderByStartTimeAsc(Long groupId, DayOfWeek dayOfWeek);

    Optional<ScheduleSlot> findByScheduleSlotIdAndDeletedAtIsNull(Long scheduleSlotId);

    Optional<ScheduleSlot> findByScheduleSlotIdAndDeletedAtIsNotNull(Long scheduleSlotId);

    List<ScheduleSlot> findAllByDeletedAtIsNotNullAndDeletedAtBefore(LocalDateTime cutoff);
}
