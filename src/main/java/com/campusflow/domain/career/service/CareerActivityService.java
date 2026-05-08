package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.CareerActivityRequest;
import com.campusflow.domain.career.dto.CareerActivityResponse;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerActivityService {

    private final CareerActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<CareerActivityResponse> getAll(String username) {
        Student student = getStudent(username);
        return activityRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(CareerActivityResponse::from).toList();
    }

    public List<CareerActivityResponse> getByType(String username, ActivityType type) {
        Student student = getStudent(username);
        return activityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(student.getId(), type)
                .stream().map(CareerActivityResponse::from).toList();
    }

    public Map<String, Long> getSummary(String username) {
        Student student = getStudent(username);
        Long sid = student.getId();
        return activityRepository.findByStudentIdOrderByCreatedAtDesc(sid)
                .stream()
                .collect(Collectors.groupingBy(
                        a -> a.getType().getLabel(),
                        Collectors.counting()
                ));
    }

    @Transactional
    public CareerActivityResponse create(String username, CareerActivityRequest req) {
        Student student = getStudent(username);
        CareerActivity activity = CareerActivity.builder()
                .student(student)
                .type(req.type())
                .status(req.status())
                .title(req.title())
                .organization(req.organization())
                .targetDate(req.targetDate())
                .completedDate(req.completedDate())
                .score(req.score())
                .memo(req.memo())
                .build();
        return CareerActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional
    public CareerActivityResponse update(String username, Long id, CareerActivityRequest req) {
        Student student = getStudent(username);
        CareerActivity activity = activityRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        activity.update(req.type(), req.status(), req.title(), req.organization(),
                req.targetDate(), req.completedDate(), req.score(), req.memo());
        return CareerActivityResponse.from(activity);
    }

    @Transactional
    public void delete(String username, Long id) {
        Student student = getStudent(username);
        CareerActivity activity = activityRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        activityRepository.delete(activity);
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
