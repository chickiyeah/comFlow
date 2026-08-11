package com.campusflow.domain.jobpilot.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.jobpilot.dto.*;
import com.campusflow.domain.jobpilot.util.CharCounter;
import com.campusflow.domain.jobpilot.util.CharCounter.LengthVerdict;
import com.campusflow.domain.jobpilot.util.FlavorDetector;
import com.campusflow.domain.jobpilot.util.FlavorDetector.Flavor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * [생성] 모듈. 공고 + 프로필(+매칭) → 문항별 자소서.
 * 참조구현 generator 를 AiFacadeService 로 포팅.
 *
 * 핵심 장치(검증된 것):
 *  - 글자수 가드 루프: 생성 → 측정 → 초과면 압축 / 미달이면 확장 (최대 MAX_RETRY)
 *  - 직무 색(flavor) 주입: 같은 프로필이라도 어떤 경험을 앞세울지 조정
 *  - 정직성 가드(§3.1): 프로필 근거에 있는 사실만, 회사/개인 구분, constraints 준수
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverLetterGeneratorService {

    private final AiFacadeService aiFacadeService;

    private static final int MAX_RETRY = 3;
    private static final int DEFAULT_LIMIT = 600;   // 제한 미명시 문항의 목표 분량

    private static final String SYSTEM_TEMPLATE = """
            당신은 한국 신입/주니어 지원자의 자기소개서를 쓰는 전문 작성자입니다.
            주어진 '지원자 근거'와 '공고 정보'만으로, 지정된 문항에 대한 자소서 항목을 작성합니다.

            [절대 규칙 - 정직성]
            - 지원자 근거에 명시된 사실만 사용합니다. 없는 경험·수치·성과를 만들어내지 않습니다.
            - 회사에서 한 일과 개인적으로 한 일을 정직하게 구분합니다. 개인 프로젝트는 "개인적으로 구축/운영" 처럼 분명히 표현합니다.
            - 약점을 거짓으로 메우지 않습니다. 필요하면 정직하게 인정하고 학습 의지로 연결합니다.
            - '지원자 제약(constraints)'이 주어지면 반드시 지킵니다(예: 특정 약점은 언급하지 않음).

            [작성 방향 - 직무 색]
            %s

            [형식]
            - 존댓말, 자연스러운 한국어 산문. 군더더기·과장 없이.
            - 마크다운·제목·불릿 없이 본문만 출력합니다.
            - 글자수 목표를 지킵니다(아래 사용자 메시지에 명시).
            - 출력은 자소서 본문 텍스트만. 따옴표나 설명을 덧붙이지 않습니다.
            """;

    private static final String USER_TEMPLATE = """
            [공고 정보]
            - 회사: %s
            - 직무: %s
            - 요구 스킬: %s
            - 우대사항: %s

            [매칭 분석]
            %s

            [지원자 근거]
            %s

            [지원자 제약]
            %s

            [작성할 문항]
            "%s"

            [분량]
            - 글자수 제한: %d자 (%s)
            - 목표: %d~%d자 사이로 작성 (제한을 넘기지 말 것)

            위 문항에 대한 자소서 항목을 작성하세요. 본문만 출력합니다.
            """;

    private static final String RETRY_OVER = """
            방금 작성한 글이 %d자로 제한(%d자)을 초과했습니다.
            내용의 핵심은 유지하되 표현을 다듬어 %d자 이하로 줄여서 다시 작성하세요. 본문만 출력합니다.
            [직전 작성본]
            %s
            """;

    private static final String RETRY_SHORT = """
            방금 작성한 글이 %d자로, 목표(%d자 이상)에 못 미쳐 빈약합니다.
            지원자 근거에 있는 사실을 더 활용해 구체적으로 보강하되, 없는 내용을 만들지는 마세요.
            %d~%d자 사이로 다시 작성하세요. 본문만 출력합니다.
            [직전 작성본]
            %s
            """;

    public GenerateReport generate(JobPosting jobRaw, StudentProfileDto profileRaw, MatchReport match) {
        JobPosting job = jobRaw.normalized();
        StudentProfileDto profile = profileRaw.normalized();

        Flavor flavor = FlavorDetector.detect(job.position(), job.requiredSkills(), job.responsibilities());

        List<EssayQuestion> questions = job.essayQuestions();
        if (questions.isEmpty()) {
            questions = List.of(
                    new EssayQuestion("지원동기 및 직무 적합성", null, null),
                    new EssayQuestion("직무 관련 경험 및 역량", null, null),
                    new EssayQuestion("입사 후 포부", null, null)
            );
        }

        List<EssayResult> essays = new ArrayList<>();
        for (EssayQuestion q : questions) {
            essays.add(generateOne(q, job, profile, match, flavor));
        }

        return new GenerateReport(job.company(), job.position(), flavor.key(), flavor.label(), essays);
    }

    private EssayResult generateOne(EssayQuestion q, JobPosting job, StudentProfileDto profile,
                                    MatchReport match, Flavor flavor) {
        String system = SYSTEM_TEMPLATE.formatted(flavor.emphasis());
        Integer charLimit = q.charLimit();
        int effLimit = charLimit != null ? charLimit : DEFAULT_LIMIT;
        LengthVerdict t0 = CharCounter.check("", effLimit, q.charLimitType());

        String matchBlock = match == null ? "(매칭 분석 없음)" : buildMatchBlock(match);

        String user = USER_TEMPLATE.formatted(
                orUnknown(job.company()),
                orUnknown(job.position()),
                fmtList(job.requiredSkills()),
                fmtList(job.preferred()),
                matchBlock,
                blankIf(profile.asEvidenceBlock(), "(근거 없음)"),
                fmtList(profile.constraints()),
                q.question(),
                effLimit,
                q.charLimitType() == null ? "공백포함" : q.charLimitType(),
                t0.targetMin(), t0.targetMax()
        );

        String text = aiFacadeService.ask(system, user).trim();
        int attempts = 1;

        // 글자수 가드 루프 — 제한이 명시된 문항만
        if (charLimit != null) {
            for (int i = 0; i < MAX_RETRY; i++) {
                LengthVerdict v = CharCounter.check(text, charLimit, q.charLimitType());
                if (!v.needsRetry()) break;
                String retry = "over".equals(v.status())
                        ? RETRY_OVER.formatted(v.count(), charLimit, v.targetMax(), text)
                        : RETRY_SHORT.formatted(v.count(), v.targetMin(), v.targetMin(), v.targetMax(), text);
                text = aiFacadeService.ask(system, retry).trim();
                attempts++;
            }
        }

        String status;
        if (charLimit != null) {
            status = CharCounter.check(text, charLimit, q.charLimitType()).status();
        } else {
            status = "no_limit";
        }

        return new EssayResult(
                q.question(), text, charLimit,
                CharCounter.count(text, true), CharCounter.count(text, false),
                status, attempts, flavor.key()
        );
    }

    private String buildMatchBlock(MatchReport m) {
        StringBuilder sb = new StringBuilder();
        if (m.summary() != null && !m.summary().isBlank()) sb.append(m.summary()).append('\n');
        if (m.strengths() != null && !m.strengths().isEmpty()) {
            sb.append("부각할 강점: ");
            sb.append(String.join(", ", m.strengths().stream()
                    .map(s -> s.skill() + "(" + s.evidence() + ")").limit(6).toList()));
            sb.append('\n');
        }
        if (m.gaps() != null && !m.gaps().isEmpty()) {
            sb.append("정직하게 다룰 약점: ");
            sb.append(String.join(", ", m.gaps().stream()
                    .map(g -> g.skill() + "[" + g.severity() + "]").limit(6).toList()));
        }
        return sb.toString().trim();
    }

    private static String fmtList(List<String> items) {
        return (items == null || items.isEmpty()) ? "(명시 없음)" : String.join(", ", items);
    }

    private static String orUnknown(String s) { return (s == null || s.isBlank()) ? "(미상)" : s; }

    private static String blankIf(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
