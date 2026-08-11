package com.campusflow.domain.quiz.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.quiz.dto.*;
import com.campusflow.domain.quiz.entity.*;
import com.campusflow.domain.quiz.repository.*;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.Role;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuizAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper;

    private static final String GRADE_SYSTEM = """
            당신은 공정한 채점자입니다. 학생 답안을 채점기준/모범답안과 비교해
            0부터 배점까지의 정수 점수와 한 줄 피드백을 매기세요.
            반드시 JSON 형식만 출력: {"score": 정수, "feedback": "한 줄 피드백"}
            """;

    private static final String QUIZ_GEN_SYSTEM = """
            당신은 한국 전문대 컴퓨터정보과 교수입니다. 주어진 주제로 퀴즈 문항을 출제하세요.
            반드시 JSON 배열만 출력하고 다른 설명은 쓰지 마세요. 각 문항 형식:
            객관식: {"type":"MCQ","text":"문제","options":["보기1","보기2","보기3","보기4"],"correctAnswer":"정답보기의 0부터 시작하는 인덱스(문자열)","points":5}
            단답형: {"type":"SHORT","text":"문제","correctAnswer":"모범답안/채점기준","points":5}
            한국어로 출제하고, 정답 인덱스는 반드시 options 범위 안의 숫자 문자열로 하세요.
            """;

    // ── 교직원: 생성/수정/삭제 ────────────────────────────────
    @Transactional
    public QuizResponse create(String username, QuizRequest req) {
        User u = assertStaff(username);
        Quiz quiz = quizRepository.save(Quiz.builder()
                .courseId(req.courseId()).title(req.title()).description(req.description())
                .createdById(u.getId()).instructorName(u.getName()).active(req.active()).build());
        saveQuestions(quiz.getId(), req.questions());
        var qs = questionRepository.findByQuizIdOrderBySeqAsc(quiz.getId());
        return QuizResponse.from(quiz, qs.size(), qs.stream().mapToInt(QuizQuestion::getPoints).sum());
    }

    @Transactional
    public QuizResponse update(String username, Long id, QuizRequest req) {
        assertStaff(username);
        Quiz quiz = quizRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        quiz.update(req.title(), req.description(), req.active());
        questionRepository.deleteByQuizId(id);
        saveQuestions(id, req.questions());
        var qs = questionRepository.findByQuizIdOrderBySeqAsc(id);
        return QuizResponse.from(quiz, qs.size(), qs.stream().mapToInt(QuizQuestion::getPoints).sum());
    }

    @Transactional
    public void delete(String username, Long id) {
        assertStaff(username);
        if (!quizRepository.existsById(id)) throw new BusinessException(ErrorCode.NOT_FOUND);
        questionRepository.deleteByQuizId(id);
        quizRepository.deleteById(id);
    }

    private void saveQuestions(Long quizId, List<QuizRequest.QuestionReq> questions) {
        int seq = 0;
        for (QuizRequest.QuestionReq q : questions) {
            String optionsJson = null;
            if (q.options() != null && !q.options().isEmpty()) {
                try { optionsJson = objectMapper.writeValueAsString(q.options()); } catch (Exception ignored) {}
            }
            questionRepository.save(QuizQuestion.builder()
                    .quizId(quizId)
                    .type(q.type() != null ? q.type() : QuestionType.MCQ)
                    .text(q.text())
                    .optionsJson(optionsJson)
                    .correctAnswer(q.correctAnswer())
                    .points(q.points() != null ? q.points() : 1)
                    .seq(seq++)
                    .build());
        }
    }

    /** AI 자동 출제 — 초안(QuizRequest)만 생성하고 저장하지 않음. 교직원이 검토 후 저장. */
    public QuizRequest generateDraft(String username, QuizGenerateRequest req) {
        assertStaff(username);
        int count = req.count() != null ? Math.max(1, Math.min(10, req.count())) : 5;
        String type = req.type() == null ? "MIX" : req.type().toUpperCase();
        String typeDesc = switch (type) {
            case "MCQ" -> "전부 객관식";
            case "SHORT" -> "전부 단답형";
            default -> "객관식과 단답형을 섞어서";
        };
        String user = "주제: " + req.topic() + "\n문항 수: " + count + "개\n유형: " + typeDesc + " 출제";

        List<QuizRequest.QuestionReq> questions = new ArrayList<>();
        try {
            String raw = aiFacadeService.ask(QUIZ_GEN_SYSTEM, user).trim();
            if (raw.startsWith("```")) raw = raw.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            int s = raw.indexOf('['), e = raw.lastIndexOf(']');
            if (s >= 0 && e > s) raw = raw.substring(s, e + 1);
            List<Map<String, Object>> arr = objectMapper.readValue(raw, new TypeReference<>() {});
            for (Map<String, Object> m : arr) {
                String t = String.valueOf(m.getOrDefault("type", "MCQ")).toUpperCase();
                QuestionType qt = t.contains("SHORT") ? QuestionType.SHORT : QuestionType.MCQ;
                String text = m.get("text") != null ? m.get("text").toString() : "";
                if (text.isBlank()) continue;
                List<String> opts = null;
                String correct = m.get("correctAnswer") != null ? m.get("correctAnswer").toString() : "0";
                if (qt == QuestionType.MCQ && m.get("options") instanceof List<?> l && !l.isEmpty()) {
                    opts = l.stream().map(String::valueOf).toList();
                }
                int pts = m.get("points") instanceof Number n ? n.intValue() : 5;
                questions.add(new QuizRequest.QuestionReq(qt, text, opts, correct, pts));
            }
        } catch (Exception ex) {
            log.warn("[퀴즈] AI 자동 출제 실패: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        if (questions.isEmpty()) throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        return new QuizRequest(req.courseId(), req.topic() + " 퀴즈", "AI 자동 생성 — 검토 후 저장하세요.", true, questions);
    }

    // ── 목록/응시 ────────────────────────────────────────────
    public List<QuizResponse> list(Long courseId) {
        List<Quiz> quizzes = courseId != null
                ? quizRepository.findByCourseIdAndActiveTrueOrderByCreatedAtDesc(courseId)
                : quizRepository.findByActiveTrueOrderByCreatedAtDesc();
        return quizzes.stream().map(q -> {
            var qs = questionRepository.findByQuizIdOrderBySeqAsc(q.getId());
            return QuizResponse.from(q, qs.size(), qs.stream().mapToInt(QuizQuestion::getPoints).sum());
        }).toList();
    }

    public QuizDetailResponse getForTaking(Long id) {
        Quiz quiz = quizRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        var questions = questionRepository.findByQuizIdOrderBySeqAsc(id).stream()
                .map(q -> new QuizDetailResponse.QuestionView(
                        q.getId(), q.getType().name(), q.getText(), parseOptions(q.getOptionsJson()), q.getPoints()))
                .toList();
        return new QuizDetailResponse(quiz.getId(), quiz.getTitle(), quiz.getDescription(), questions);
    }

    // ── 학생 제출 + 채점 ─────────────────────────────────────
    @Transactional
    public QuizResultResponse submit(String username, Long quizId, QuizSubmitRequest req) {
        quizRepository.findById(quizId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Student student = getStudent(username);
        var questions = questionRepository.findByQuizIdOrderBySeqAsc(quizId);
        Map<Long, String> responses = new java.util.HashMap<>();
        if (req.answers() != null) req.answers().forEach(a -> responses.put(a.questionId(), a.response()));

        // 기존 제출 있으면 재응시 → 교체
        submissionRepository.findByQuizIdAndStudentId(quizId, student.getId()).ifPresent(prev -> {
            answerRepository.deleteAll(answerRepository.findBySubmissionId(prev.getId()));
            submissionRepository.delete(prev);
        });

        int total = 0, max = 0;
        boolean needsReview = false;
        List<QuizAnswer> graded = new ArrayList<>();
        List<QuizResultResponse.AnswerResult> results = new ArrayList<>();

        for (QuizQuestion q : questions) {
            max += q.getPoints();
            String resp = responses.getOrDefault(q.getId(), "");
            int awarded; boolean ai = false; String feedback;
            if (q.getType() == QuestionType.MCQ) {
                boolean correct = resp != null && resp.trim().equals(
                        q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim() : "");
                awarded = correct ? q.getPoints() : 0;
                feedback = correct ? "정답" : "오답";
            } else {
                var ag = gradeShort(q, resp);
                awarded = ag[0] == null ? 0 : (int) ag[0];
                feedback = (String) ag[1];
                ai = true; needsReview = true;
            }
            total += awarded;
            graded.add(QuizAnswer.builder().questionId(q.getId()).response(resp)
                    .awardedScore(awarded).aiGraded(ai).feedback(feedback).build());
            results.add(new QuizResultResponse.AnswerResult(q.getId(), awarded, q.getPoints(), ai, feedback));
        }

        QuizSubmission sub = submissionRepository.save(QuizSubmission.builder()
                .quizId(quizId).studentId(student.getId()).studentName(student.getName())
                .score(total).maxScore(max).needsReview(needsReview).build());
        for (QuizAnswer a : graded) {
            answerRepository.save(QuizAnswer.builder().submissionId(sub.getId()).questionId(a.getQuestionId())
                    .response(a.getResponse()).awardedScore(a.getAwardedScore())
                    .aiGraded(a.isAiGraded()).feedback(a.getFeedback()).build());
        }
        return new QuizResultResponse(total, max, needsReview, results);
    }

    /** 단답 AI 채점 → [score(Integer), feedback(String)] */
    private Object[] gradeShort(QuizQuestion q, String response) {
        if (response == null || response.isBlank()) return new Object[]{0, "미응답"};
        try {
            String user = "문항: " + q.getText()
                    + "\n채점기준/모범답안: " + (q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "(없음)")
                    + "\n배점: " + q.getPoints() + "점\n학생답안: " + response;
            String raw = aiFacadeService.ask(GRADE_SYSTEM, user).trim();
            if (raw.startsWith("```")) raw = raw.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            int s = raw.indexOf('{'), e = raw.lastIndexOf('}');
            if (s >= 0 && e > s) raw = raw.substring(s, e + 1);
            Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<>() {});
            int score = parsed.get("score") instanceof Number n ? n.intValue() : 0;
            score = Math.max(0, Math.min(q.getPoints(), score));
            String fb = parsed.get("feedback") != null ? parsed.get("feedback").toString() : "AI 채점";
            return new Object[]{score, "🤖 " + fb};
        } catch (Exception ex) {
            log.warn("[퀴즈] AI 단답 채점 실패: {}", ex.getMessage());
            return new Object[]{0, "AI 채점 실패 — 교수 검토 필요"};
        }
    }

    public QuizResultResponse myResult(String username, Long quizId) {
        Student student = getStudent(username);
        QuizSubmission sub = submissionRepository.findByQuizIdAndStudentId(quizId, student.getId()).orElse(null);
        if (sub == null) return null;
        var answers = answerRepository.findBySubmissionId(sub.getId());
        var qpoints = new java.util.HashMap<Long, Integer>();
        questionRepository.findByQuizIdOrderBySeqAsc(quizId).forEach(q -> qpoints.put(q.getId(), q.getPoints()));
        var results = answers.stream().map(a -> new QuizResultResponse.AnswerResult(
                a.getQuestionId(), a.getAwardedScore(), qpoints.getOrDefault(a.getQuestionId(), 0),
                a.isAiGraded(), a.getFeedback())).toList();
        return new QuizResultResponse(sub.getScore(), sub.getMaxScore(), sub.isNeedsReview(), results);
    }

    // ── 교직원: 제출 목록 + 채점 검토(override) ───────────────
    public List<StaffSubmissionResponse> submissions(String username, Long quizId) {
        assertStaff(username);
        return submissionRepository.findByQuizIdOrderBySubmittedAtDesc(quizId)
                .stream().map(StaffSubmissionResponse::from).toList();
    }

    @Transactional
    public void overrideAnswer(String username, Long answerId, int score, String feedback) {
        assertStaff(username);
        QuizAnswer a = answerRepository.findById(answerId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        a.override(Math.max(0, score), feedback);
        // 제출 총점 재계산
        QuizSubmission sub = submissionRepository.findById(a.getSubmissionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        var subAnswers = answerRepository.findBySubmissionId(sub.getId());
        int total = subAnswers.stream().mapToInt(QuizAnswer::getAwardedScore).sum();
        boolean stillAi = subAnswers.stream().anyMatch(QuizAnswer::isAiGraded);
        sub.applyScore(total, stillAi);
    }

    // ── helpers ──────────────────────────────────────────────
    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }

    private User assertStaff(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() != Role.ROLE_ADMIN && user.getRole() != Role.ROLE_PROFESSOR)
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return user;
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
