package com.campusflow.domain.gamification.service;

import com.campusflow.domain.award.repository.AwardRepository;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.course.entity.CourseView;
import com.campusflow.domain.course.repository.CourseViewRepository;
import com.campusflow.domain.gamification.dto.GamificationResponse;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.quiz.entity.QuizSubmission;
import com.campusflow.domain.quiz.repository.QuizSubmissionRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationService {

    private static final int LEVEL_SIZE = 100;   // 레벨당 포인트

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseViewRepository courseViewRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final CareerActivityRepository careerActivityRepository;
    private final AwardRepository awardRepository;
    private final GradeRepository gradeRepository;

    public GamificationResponse getMyStatus(String username) {
        Student student = getStudent(username);
        Long sid = student.getId();

        List<CourseView> views = courseViewRepository.findByStudentId(sid);
        int completions = (int) views.stream().filter(CourseView::isCompleted).count();

        List<QuizSubmission> subs = quizSubmissionRepository.findByStudentId(sid);
        int quizzes = subs.size();
        int perfect = (int) subs.stream().filter(q -> q.getMaxScore() > 0 && q.getScore() == q.getMaxScore()).count();

        int activities = careerActivityRepository.findByStudentIdOrderByCreatedAtDesc(sid).size();
        int awards = awardRepository.findByStudentIdOrderByAwardDateDesc(sid).size();
        Double gpaObj = gradeRepository.calculateGpa(sid);
        double gpa = gpaObj != null ? Math.round(gpaObj * 100.0) / 100.0 : 0.0;

        // 포인트 합산
        int points = completions * 50 + quizzes * 20 + perfect * 30 + activities * 15 + awards * 50;
        int level = points / LEVEL_SIZE + 1;
        int levelProgress = (int) Math.round((points % LEVEL_SIZE) * 100.0 / LEVEL_SIZE);
        int toNextLevel = LEVEL_SIZE - (points % LEVEL_SIZE);

        // 배지 (획득 조건)
        List<GamificationResponse.Badge> badges = List.of(
                new GamificationResponse.Badge("FIRST_COURSE", "school",             completions >= 1),
                new GamificationResponse.Badge("DILIGENT",     "menu_book",          completions >= 3),
                new GamificationResponse.Badge("QUIZ_TAKER",   "quiz",               quizzes >= 1),
                new GamificationResponse.Badge("QUIZ_MASTER",  "workspace_premium",  perfect >= 1),
                new GamificationResponse.Badge("CHALLENGER",   "flag",               activities >= 1),
                new GamificationResponse.Badge("SCHOLAR",      "military_tech",      gpa >= 3.5)
        );

        return new GamificationResponse(points, level, levelProgress, toNextLevel,
                new GamificationResponse.Stats(completions, quizzes, perfect, activities, awards, gpa),
                badges);
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
