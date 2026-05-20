package com.campusflow.domain.schedule.service;

import com.campusflow.domain.schedule.dto.ScheduleRequest;
import com.campusflow.domain.schedule.dto.ScheduleResponse;
import com.campusflow.domain.schedule.entity.Schedule;
import com.campusflow.domain.schedule.repository.ScheduleRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<ScheduleResponse> getAll(String username) {
        return scheduleRepository
                .findByStudentIdOrderByDayOfWeekAscStartTimeAsc(getStudent(username).getId())
                .stream().map(ScheduleResponse::from).toList();
    }

    public List<ScheduleResponse> getToday(String username) {
        return scheduleRepository
                .findByStudentIdAndDayOfWeekOrderByStartTimeAsc(
                        getStudent(username).getId(), LocalDate.now().getDayOfWeek())
                .stream().map(ScheduleResponse::from).toList();
    }

    @Transactional
    public ScheduleResponse create(String username, ScheduleRequest req) {
        Student student = getStudent(username);
        Schedule schedule = new Schedule(
                student, req.subjectName(), req.subjectCode(),
                req.professor(), req.room(), req.dayOfWeek(),
                req.startTime(), req.endTime(), req.year(), req.semester()
        );
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(String username, Long id, ScheduleRequest req) {
        Schedule schedule = getOwned(username, id);
        schedule.update(req.subjectName(), req.subjectCode(), req.professor(),
                req.room(), req.dayOfWeek(), req.startTime(), req.endTime(),
                req.year(), req.semester());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(String username, Long id) {
        scheduleRepository.delete(getOwned(username, id));
    }

    private Schedule getOwned(String username, Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!schedule.getStudent().getId().equals(getStudent(username).getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return schedule;
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
