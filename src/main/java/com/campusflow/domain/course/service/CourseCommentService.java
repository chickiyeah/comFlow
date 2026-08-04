package com.campusflow.domain.course.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.course.dto.CourseCommentRequest;
import com.campusflow.domain.course.dto.CourseCommentResponse;
import com.campusflow.domain.course.entity.CourseComment;
import com.campusflow.domain.course.entity.OnlineCourse;
import com.campusflow.domain.course.repository.CourseCommentRepository;
import com.campusflow.domain.course.repository.OnlineCourseRepository;
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
public class CourseCommentService {

    private final CourseCommentRepository commentRepository;
    private final OnlineCourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AiFacadeService aiFacadeService;

    private static final String TA_SYSTEM = """
            당신은 온라인 강좌의 친절한 AI 조교입니다.
            강좌 내용과 관련된 학생 질문에 정확하고 간결하게(3~5문장) 답하세요.
            확실하지 않으면 추측하지 말고 담당 강사에게 문의를 권하세요.
            """;

    @Transactional(readOnly = true)
    public List<CourseCommentResponse> list(Long courseId) {
        return commentRepository.findByCourseIdOrderByCreatedAtAsc(courseId)
                .stream().map(CourseCommentResponse::from).toList();
    }

    @Transactional
    public List<CourseCommentResponse> add(String username, Long courseId, CourseCommentRequest req) {
        OnlineCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        boolean staff = user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_PROFESSOR;

        commentRepository.save(CourseComment.builder()
                .courseId(courseId)
                .parentId(req.parentId())
                .authorUserId(user.getId())
                .authorName(user.getName())
                .content(req.content())
                .question(req.question())
                .staff(staff)
                .aiGenerated(false)
                .build());

        // 최상위 질문이면 AI 조교가 자동 답변 (best-effort) — 강좌 × AI 연동
        if (req.question() && req.parentId() == null) {
            try {
                String ctx = "[강좌] " + course.getTitle()
                        + (course.getDescription() != null ? "\n" + course.getDescription() : "")
                        + "\n\n[질문] " + req.content();
                String answer = aiFacadeService.ask(TA_SYSTEM, ctx);
                if (answer != null && !answer.isBlank()) {
                    commentRepository.save(CourseComment.builder()
                            .courseId(courseId)
                            .parentId(null)
                            .authorUserId(null)
                            .authorName("AI 조교")
                            .content(answer.trim())
                            .question(false)
                            .staff(false)
                            .aiGenerated(true)
                            .build());
                }
            } catch (Exception e) {
                log.warn("[강좌 Q&A] AI 답변 생성 실패: {}", e.getMessage());
            }
        }
        return list(courseId);
    }

    @Transactional
    public void delete(String username, Long commentId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        CourseComment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        boolean staff = user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_PROFESSOR;
        boolean owner = user.getId().equals(c.getAuthorUserId());
        if (!staff && !owner) throw new BusinessException(ErrorCode.FORBIDDEN);
        commentRepository.delete(c);
    }
}
