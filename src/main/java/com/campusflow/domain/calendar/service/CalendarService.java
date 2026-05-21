package com.campusflow.domain.calendar.service;

import com.campusflow.domain.calendar.dto.CalendarEvent;
import com.campusflow.domain.notice.repository.NoticeRepository;
import com.campusflow.domain.schedule.entity.Schedule;
import com.campusflow.domain.schedule.repository.ScheduleRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final ScheduleRepository scheduleRepo;
    private final NoticeRepository   noticeRepo;
    private final UserRepository     userRepo;
    private final StudentRepository  studentRepo;

    @Cacheable(value = "calendarEvents", key = "#username + '_' + #year + '_' + #month")
    public List<CalendarEvent> getMonthEvents(String username, int year, int month) {
        Student student = getStudent(username);
        List<CalendarEvent> events = new ArrayList<>();

        YearMonth ym = YearMonth.of(year, month);
        List<Schedule> schedules = scheduleRepo.findByStudentIdOrderByDayOfWeekAscStartTimeAsc(student.getId());

        // 강의시간표 → 해당 월의 날짜로 펼치기
        for (LocalDate date = ym.atDay(1); !date.isAfter(ym.atEndOfMonth()); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            for (Schedule s : schedules) {
                if (s.getDayOfWeek() == dow) {
                    events.add(new CalendarEvent(
                            date,
                            s.getSubjectName(),
                            "LECTURE",
                            s.getStartTime() + " ~ " + s.getEndTime() + " | " + (s.getRoom().isBlank() ? "강의실 미정" : s.getRoom()),
                            "#00236f"
                    ));
                }
            }
        }

        // 공지사항 날짜
        noticeRepo.findAllByOrderByImportantDescCreatedAtDesc().forEach(n -> {
            LocalDate d = n.getCreatedAt().toLocalDate();
            if (d.getYear() == year && d.getMonthValue() == month) {
                events.add(new CalendarEvent(
                        d, n.getTitle(), "NOTICE",
                        n.getSummary() != null ? n.getSummary() : "",
                        n.isImportant() ? "#dc2626" : "#6b7280"
                ));
            }
        });

        events.sort((a, b) -> a.date().compareTo(b.date()));
        return events;
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
