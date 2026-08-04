package com.campusflow.domain.course.service;

import com.campusflow.domain.course.dto.CourseProgressResponse;
import com.campusflow.domain.course.dto.CourseRequest;
import com.campusflow.domain.course.dto.CourseResponse;
import com.campusflow.domain.course.entity.CourseView;
import com.campusflow.domain.course.entity.OnlineCourse;
import com.campusflow.domain.career.entity.ActivityStatus;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.course.repository.CourseViewRepository;
import com.campusflow.domain.course.repository.OnlineCourseRepository;
import com.campusflow.domain.notification.entity.NotificationType;
import com.campusflow.domain.notification.service.NotificationService;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.Role;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final OnlineCourseRepository courseRepository;
    private final CourseViewRepository viewRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final CareerActivityRepository careerActivityRepository;
    private final PdfService pdfService;

    public List<CourseResponse> listActive() {
        return courseRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().map(CourseResponse::from).toList();
    }

    /** 교직원 관리용 — 비활성 포함 전체 */
    public List<CourseResponse> listAll(String username) {
        assertStaff(username);
        return courseRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(CourseResponse::from).toList();
    }

    public CourseResponse get(Long id) {
        return CourseResponse.from(courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
    }

    @Transactional
    public CourseResponse create(String username, CourseRequest req) {
        User u = assertStaff(username);
        OnlineCourse course = courseRepository.save(OnlineCourse.builder()
                .title(req.title()).description(req.description()).videoUrl(req.videoUrl())
                .thumbnailUrl(req.thumbnailUrl()).category(req.category())
                .instructorName(u.getName()).createdById(u.getId()).active(req.active()).build());

        // 새 강좌 등록 → 전체 학생 인앱 알림 + 웹푸시 broadcast
        int sent = 0;
        for (Student s : studentRepository.findAll()) {
            try {
                notificationService.create(s, NotificationType.NOTICE,
                        "새 온라인 강좌: " + req.title(),
                        (req.description() != null && !req.description().isBlank())
                                ? req.description() : "새 강좌가 등록되었습니다.",
                        "/courses");
                sent++;
            } catch (Exception e) {
                log.warn("[강좌 broadcast] 학생 {} 알림 실패: {}", s.getId(), e.getMessage());
            }
        }
        log.info("[강좌 broadcast] '{}' — {}명에게 발송", req.title(), sent);
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse update(String username, Long id, CourseRequest req) {
        assertStaff(username);
        OnlineCourse c = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        c.update(req.title(), req.description(), req.videoUrl(), req.thumbnailUrl(), req.category(), req.active());
        return CourseResponse.from(c);
    }

    @Transactional
    public void delete(String username, Long id) {
        assertStaff(username);
        if (!courseRepository.existsById(id)) throw new BusinessException(ErrorCode.NOT_FOUND);
        courseRepository.deleteById(id);
    }

    /** 학생 시청 기록 — 강좌별 최초 1회만 조회수 증가 */
    @Transactional
    public void recordView(String username, Long courseId) {
        OnlineCourse c = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        userRepository.findByUsername(username).ifPresent(u ->
                studentRepository.findByUserId(u.getId()).ifPresent(st -> {
                    if (!viewRepository.existsByCourseIdAndStudentId(courseId, st.getId())) {
                        viewRepository.save(CourseView.builder().courseId(courseId).studentId(st.getId()).build());
                        c.increaseView();
                    }
                }));
    }

    /**
     * 수강 완료 처리 — CourseView를 완료로 표시하고, 최초 완료 시 취업활동(교육/연수)에 자동 기록.
     * 강좌 × 취업 트래킹 연동.
     */
    @Transactional
    public void complete(String username, Long courseId) {
        OnlineCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Student student = studentRepository.findByUserId(u.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        CourseView view = viewRepository.findByCourseIdAndStudentId(courseId, student.getId())
                .orElseGet(() -> viewRepository.save(
                        CourseView.builder().courseId(courseId).studentId(student.getId()).build()));
        awardCompletion(student, course, view);
    }

    /** 진도 저장 — 이어보기 위치/길이 기록, 90% 이상이면 자동 수강완료. */
    @Transactional
    public void saveProgress(String username, Long courseId, int positionSec, int durationSec) {
        OnlineCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Student student = studentRepository.findByUserId(u.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        CourseView view = viewRepository.findByCourseIdAndStudentId(courseId, student.getId())
                .orElseGet(() -> viewRepository.save(
                        CourseView.builder().courseId(courseId).studentId(student.getId()).build()));
        view.updateProgress(positionSec, durationSec);
        if (durationSec > 0 && positionSec >= durationSec * 0.9) {
            awardCompletion(student, course, view);
        }
    }

    /** 내 강좌 진도 목록 */
    @Transactional(readOnly = true)
    public List<CourseProgressResponse> myProgress(String username) {
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) return List.of();
        Student st = studentRepository.findByUserId(u.getId()).orElse(null);
        if (st == null) return List.of();
        return viewRepository.findByStudentId(st.getId()).stream()
                .map(v -> new CourseProgressResponse(v.getCourseId(), v.getLastPositionSec(), v.getDurationSec(), v.isCompleted()))
                .toList();
    }

    /** 최초 완료 시에만 취업활동(교육/연수) 자동 등록 */
    private void awardCompletion(Student student, OnlineCourse course, CourseView view) {
        if (view.isCompleted()) return;
        view.markCompleted();
        careerActivityRepository.save(CareerActivity.builder()
                .student(student)
                .type(ActivityType.TRAINING)
                .status(ActivityStatus.COMPLETED)
                .title("[온라인 강좌] " + course.getTitle())
                .organization("CampusFlow 온라인 강좌")
                .completedDate(java.time.LocalDate.now())
                .memo("강좌 수강 완료 자동 기록")
                .build());
    }

    /** 수료증 PDF — 수강 완료한 강좌에 한해 발급. */
    public byte[] generateCertificate(String username, Long courseId) {
        OnlineCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Student student = studentRepository.findByUserId(u.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        CourseView view = viewRepository.findByCourseIdAndStudentId(courseId, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!view.isCompleted()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);   // 미완료 강좌는 발급 불가
        }
        java.util.Map<String, Object> model = new java.util.HashMap<>();
        model.put("studentName", student.getName());
        model.put("courseTitle", course.getTitle());
        model.put("instructorName", course.getInstructorName());
        model.put("completedDate", view.getWatchedAt().toLocalDate().toString());
        model.put("certNo", String.format("CF-%d-%d", courseId, student.getId()));
        return pdfService.generateCertificatePdf(model);
    }

    private User assertStaff(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() != Role.ROLE_ADMIN && user.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }
}
