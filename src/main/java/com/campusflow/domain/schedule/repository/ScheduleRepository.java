package com.campusflow.domain.schedule.repository;

import com.campusflow.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByStudentIdOrderByDayOfWeekAscStartTimeAsc(Long studentId);
    List<Schedule> findByStudentIdAndDayOfWeekOrderByStartTimeAsc(Long studentId, DayOfWeek dayOfWeek);
    List<Schedule> findByStudentIdAndYearAndSemesterOrderByDayOfWeekAscStartTimeAsc(Long studentId, int year, int semester);
}
