package com.campusflow.domain.assignment.service;

import com.campusflow.domain.ai.service.AiTextService;
import com.campusflow.domain.assignment.dto.AiCheckResponse;
import com.campusflow.domain.assignment.entity.Assignment;
import com.campusflow.domain.assignment.entity.Submission;
import com.campusflow.domain.assignment.repository.SubmissionRepository;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 과제 제출물 AI 완성도 점검(교사 전용). 저장하지 않고 제안 점수·피드백만 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentAiService {

    private static final String AI_CHECK_SYSTEM = """
            당신은 과제 제출물 평가 도우미입니다. 과제 지시사항과 배점을 기준으로 학생 제출물을 평가하세요.
            반드시 JSON만 출력하고 다른 설명은 쓰지 마세요:
            {"suggestedScore": 정수(0~배점), "feedback": "한두 문장 총평", "strengths": ["강점1","강점2"], "improvements": ["개선점1","개선점2"]}
            한국어로 작성하세요.
            """;

    private final SubmissionRepository submissionRepository;
    private final ClassAccessService classAccess;
    private final AiTextService aiTextService;
    private final ObjectMapper objectMapper;

    public AiCheckResponse check(String username, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Assignment assignment = submission.getAssignment();
        classAccess.requireTeacher(assignment.getClassRoom().getId(), username);

        String content = submission.getContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String user = "과제 지시사항: " + (assignment.getInstructions() != null ? assignment.getInstructions() : "(없음)")
                + "\n배점: " + assignment.getPoints() + "점"
                + "\n학생 제출물:\n" + content;

        try {
            String raw = clean(aiTextService.ask(AI_CHECK_SYSTEM, user));
            int s = raw.indexOf('{'), e = raw.lastIndexOf('}');
            if (s >= 0 && e > s) {
                raw = raw.substring(s, e + 1);
            }
            Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<>() {});
            Integer score = parsed.get("suggestedScore") instanceof Number n
                    ? Math.max(0, Math.min(assignment.getPoints(), n.intValue())) : null;
            String feedback = parsed.get("feedback") != null ? parsed.get("feedback").toString() : null;
            return new AiCheckResponse(score, feedback, toStringList(parsed.get("strengths")),
                    toStringList(parsed.get("improvements")));
        } catch (Exception ex) {
            log.warn("[과제AI] ai-check 실패: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String clean(String raw) {
        String r = raw == null ? "" : raw.trim();
        if (r.startsWith("```")) {
            r = r.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        return r;
    }
}
