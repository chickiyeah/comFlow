package com.campusflow.domain.interview.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.career.entity.SavedJob;
import com.campusflow.domain.career.repository.SavedJobRepository;
import com.campusflow.domain.interview.dto.InterviewResponse;
import com.campusflow.domain.interview.dto.StartInterviewRequest;
import com.campusflow.domain.interview.dto.SubmitAnswerRequest;
import com.campusflow.domain.interview.entity.InterviewQuestion;
import com.campusflow.domain.interview.entity.InterviewSession;
import com.campusflow.domain.interview.entity.InterviewStatus;
import com.campusflow.domain.interview.repository.InterviewQuestionRepository;
import com.campusflow.domain.interview.repository.InterviewSessionRepository;
import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final AiFacadeService aiFacadeService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PortfolioRepository portfolioRepository;
    private final SavedJobRepository savedJobRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;

    // ──────────────────────────────────────────────────────────────
    // 1. 세션 시작 — 포트폴리오 분석 후 첫 질문 생성
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public InterviewResponse.SessionStarted startSession(String username, StartInterviewRequest req) {
        Student student = getStudent(username);

        // 회사/직무 결정 (저장된 공고 or 직접 입력)
        String company  = req.getCompany();
        String position = req.getPosition();
        if (req.getSavedJobId() != null) {
            SavedJob job = savedJobRepository.findById(req.getSavedJobId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            company  = job.getCompany();
            position = job.getTitle();
        }

        int total = (req.getTotalQuestions() > 0 && req.getTotalQuestions() <= 10)
                ? req.getTotalQuestions() : 5;

        // 세션 생성
        InterviewSession session = sessionRepository.save(
                InterviewSession.builder()
                        .student(student)
                        .company(company)
                        .position(position)
                        .totalQuestions(total)
                        .build()
        );

        // 포트폴리오 컨텍스트 조회
        String portfolioContext = buildPortfolioContext(student.getId());

        // 첫 번째 질문 AI 생성
        String firstQuestion = generateQuestion(company, position, portfolioContext, 0, total, List.of());
        InterviewQuestion q = questionRepository.save(
                InterviewQuestion.builder()
                        .session(session)
                        .questionIndex(0)
                        .question(firstQuestion)
                        .build()
        );

        return InterviewResponse.SessionStarted.builder()
                .sessionId(session.getId())
                .company(company)
                .position(position)
                .totalQuestions(total)
                .currentIndex(0)
                .question(q.getQuestion())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // 2. 답변 제출 — 피드백 생성 + 다음 질문 or 종합 피드백
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public InterviewResponse.AnswerResult submitAnswer(String username, Long sessionId, SubmitAnswerRequest req) {
        Student student = getStudent(username);
        InterviewSession session = loadOwnedSession(student, sessionId);

        if (session.getStatus() == InterviewStatus.FINISHED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        int idx = session.getCurrentIndex();

        // 현재 질문에 답변 저장
        InterviewQuestion current = questionRepository
                .findBySessionIdAndQuestionIndex(sessionId, idx)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        current.submitAnswer(req.getAnswer());

        // 단건 피드백 생성
        String feedback = generateAnswerFeedback(
                session.getCompany(), session.getPosition(),
                current.getQuestion(), req.getAnswer()
        );
        current.setFeedback(feedback);

        // 진행 상태 업데이트
        session.nextQuestion();

        // 마지막 답변이면 종합 피드백 생성 후 세션 종료
        if (session.isFinished()) {
            List<InterviewQuestion> allQuestions = questionRepository
                    .findBySessionIdOrderByQuestionIndex(sessionId);
            String overallFeedback = generateOverallFeedback(
                    session.getCompany(), session.getPosition(), allQuestions);
            session.finish(overallFeedback);

            return InterviewResponse.AnswerResult.builder()
                    .sessionId(sessionId)
                    .currentIndex(idx)
                    .feedback(feedback)
                    .finished(true)
                    .overallFeedback(overallFeedback)
                    .build();
        }

        // 다음 질문 생성
        String portfolioContext = buildPortfolioContext(student.getId());
        List<String> previousQuestions = questionRepository
                .findBySessionIdOrderByQuestionIndex(sessionId)
                .stream().map(InterviewQuestion::getQuestion).toList();

        String nextQuestion = generateQuestion(
                session.getCompany(), session.getPosition(),
                portfolioContext, session.getCurrentIndex(),
                session.getTotalQuestions(), previousQuestions
        );
        questionRepository.save(
                InterviewQuestion.builder()
                        .session(session)
                        .questionIndex(session.getCurrentIndex())
                        .question(nextQuestion)
                        .build()
        );

        return InterviewResponse.AnswerResult.builder()
                .sessionId(sessionId)
                .currentIndex(session.getCurrentIndex())
                .feedback(feedback)
                .nextQuestion(nextQuestion)
                .finished(false)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 세션 목록 조회
    // ──────────────────────────────────────────────────────────────
    public List<InterviewResponse.SessionSummary> listSessions(String username) {
        Student student = getStudent(username);
        return sessionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(InterviewResponse.SessionSummary::from).toList();
    }

    // ──────────────────────────────────────────────────────────────
    // 4. 세션 상세 조회 (결과 확인)
    // ──────────────────────────────────────────────────────────────
    public InterviewResponse.SessionDetail getSessionDetail(String username, Long sessionId) {
        Student student = getStudent(username);
        InterviewSession session = loadOwnedSession(student, sessionId);

        List<InterviewResponse.SessionDetail.QuestionItem> items =
                questionRepository.findBySessionIdOrderByQuestionIndex(sessionId)
                        .stream().map(InterviewResponse.SessionDetail.QuestionItem::from).toList();

        return InterviewResponse.SessionDetail.builder()
                .sessionId(session.getId())
                .company(session.getCompany())
                .position(session.getPosition())
                .status(session.getStatus().name())
                .overallFeedback(session.getOverallFeedback())
                .questions(items)
                .createdAt(session.getCreatedAt())
                .finishedAt(session.getFinishedAt())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    /** 포트폴리오 목록을 프롬프트용 텍스트로 변환 */
    private String buildPortfolioContext(Long studentId) {
        List<Portfolio> portfolios = portfolioRepository.findByStudentIdOrderByStartDateDesc(studentId);
        if (portfolios.isEmpty()) return "등록된 포트폴리오 없음";

        return portfolios.stream().map(p ->
                String.format("- [%s] 역할: %s | 기술: %s | 설명: %s",
                        p.getTitle(), p.getRole(),
                        p.getTechStack() != null ? p.getTechStack() : "없음",
                        p.getDescription() != null ? p.getDescription() : "없음")
        ).collect(Collectors.joining("\n"));
    }

    /** 면접 질문 1개 생성 */
    private String generateQuestion(String company, String position,
                                    String portfolioContext, int currentIdx,
                                    int total, List<String> previousQuestions) {
        String prevStr = previousQuestions.isEmpty() ? "없음"
                : previousQuestions.stream()
                    .map(q -> "- " + q)
                    .collect(Collectors.joining("\n"));

        String system = """
                당신은 경험 많은 IT 기업 채용 면접관입니다.
                지원자의 포트폴리오를 바탕으로 실제 면접에서 나올법한 질문을 생성합니다.
                반드시 순수한 한국어로만 답변하세요. 한자, 영어, 일본어 등 다른 언어는 절대 혼용하지 마세요.
                질문 하나만 출력하세요. 설명, 번호, 따옴표 없이 질문 문장만 출력하세요.
                """;

        String user = String.format("""
                회사: %s
                직무: %s
                지원자 포트폴리오:
                %s
                
                이미 한 질문 목록 (중복 금지):
                %s
                
                현재 %d번째 질문 (전체 %d개).
                포트폴리오와 직무에 연관된 구체적인 기술/경험 질문을 생성하세요.
                인성 질문과 기술 질문을 번갈아 출제하면 좋습니다.
                """, company, position, portfolioContext, prevStr, currentIdx + 1, total);

        try {
            return aiFacadeService.ask(system, user).strip();
        } catch (Exception e) {
            log.error("[Interview] 질문 생성 실패", e);
            return "본인의 가장 자신 있는 프로젝트 경험을 구체적으로 설명해 주세요.";
        }
    }

    /** 답변에 대한 단건 피드백 생성 */
    private String generateAnswerFeedback(String company, String position,
                                          String question, String answer) {
        String system = """
                당신은 IT 기업 면접관입니다.
                지원자의 답변을 듣고 짧고 실용적인 피드백을 한국어로 제공합니다.
                반드시 순수한 한국어로만 답변하세요. 한자, 영어 등 다른 언어는 절대 혼용하지 마세요.
                잘한 점 1가지와 보완할 점 1가지를 포함해 3~5문장으로 작성하세요.
                점수나 평점은 매기지 않습니다.
                """;

        String user = String.format("""
                회사: %s / 직무: %s
                질문: %s
                지원자 답변: %s
                """, company, position, question, answer);

        try {
            return aiFacadeService.ask(system, user).strip();
        } catch (Exception e) {
            log.error("[Interview] 피드백 생성 실패", e);
            return "답변을 잘 작성해 주셨습니다. 구체적인 수치와 결과를 함께 제시하면 더욱 인상적인 답변이 됩니다.";
        }
    }

    /** 전체 면접 종합 피드백 생성 */
    private String generateOverallFeedback(String company, String position,
                                           List<InterviewQuestion> questions) {
        String qna = questions.stream().map(q ->
                String.format("Q: %s\nA: %s", q.getQuestion(),
                        q.getAnswer() != null ? q.getAnswer() : "(미답변)")
        ).collect(Collectors.joining("\n\n"));

        String system = """
                당신은 IT 기업 채용 면접관입니다.
                지원자의 전체 면접 내용을 종합하여 최종 피드백을 한국어로 작성합니다.
                반드시 순수한 한국어로만 답변하세요. 한자, 영어 등 다른 언어는 절대 혼용하지 마세요.
                아래 항목을 포함해 200~300자로 작성하세요:
                1. 전반적인 인상 (강점 2가지)
                2. 개선이 필요한 부분 (1~2가지)
                3. 실제 취업 준비를 위한 구체적인 조언 1가지
                마크다운 없이 자연스러운 문단으로 작성하세요.
                """;

        String user = String.format("""
                회사: %s / 직무: %s
                
                전체 질문과 답변:
                %s
                """, company, position, qna);

        try {
            return aiFacadeService.ask(system, user).strip();
        } catch (Exception e) {
            log.error("[Interview] 종합 피드백 생성 실패", e);
            return "면접 수고하셨습니다. 전반적으로 성실하게 답변하셨으며, 프로젝트 경험을 더 구체적인 수치와 함께 준비하면 실제 면접에서 더욱 좋은 인상을 남길 수 있습니다.";
        }
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private InterviewSession loadOwnedSession(Student student, Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!session.getStudent().getId().equals(student.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return session;
    }
}
