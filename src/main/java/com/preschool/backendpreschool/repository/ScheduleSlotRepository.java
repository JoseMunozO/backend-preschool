package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    List<ScheduleSlot> findByClassGroupGroupIdOrderByDayOfWeekAscStartTimeAsc(Long groupId);

    List<ScheduleSlot> findByDayOfWeekOrderByStartTimeAsc(DayOfWeek dayOfWeek);

    List<ScheduleSlot> findByClassGroupGroupIdAndDayOfWeekOrderByStartTimeAsc(Long groupId, DayOfWeek dayOfWeek);
}
