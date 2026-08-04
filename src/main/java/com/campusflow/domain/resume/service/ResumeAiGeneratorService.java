package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.jobpilot.util.CharCounter;
import com.campusflow.domain.jobpilot.util.CharCounter.LengthVerdict;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeData.*;
import com.campusflow.domain.resume.dto.ResumeDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A 단계 — 표준(general) 이력서 자동생성.
 * 조립된 사실 + AI 자기소개(글자수 가드) + 정직성 검증(자동 재생성) → ResumeDraft(미저장).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiGeneratorService {

    private final ResumeAssembler assembler;
    private final HonestyVerifier honestyVerifier;
    private final AiFacadeService aiFacadeService;

    private static final int MAX_LEN_RETRY = 3;
    private static final int TARGET_LIMIT = 1000;               // 목표 상한
    private static final String LIMIT_TYPE = "공백포함";

    // 일반기업 자소서 4대 항목 (딥리서치 근거)
    private static final List<String> GENERAL_QUESTIONS =
            List.of("성장과정", "성격의 장단점", "지원동기", "입사 후 포부");

    private static final String SYSTEM = """
            당신은 한국 신입/전문대 지원자의 이력서 자기소개서를 쓰는 전문 작성자입니다.
            '지원자 근거'에 있는 사실만으로 지정 문항의 자소서를 작성합니다.

            [절대 규칙 - 정직성]
            - 근거에 없는 경험·수치·자격·회사·성과를 만들어내지 않습니다.
            - 회사에서 한 일과 개인/학과 프로젝트를 정직하게 구분합니다.
            - 성과가 작아도 배운 점·직무 연관성으로 연결합니다(자기과소평가·과장 모두 금지).

            [형식]
            - 존댓말, 자연스러운 한국어 산문. 진부한 생활신조·반복·오탈자 금지.
            - 마크다운·제목·불릿 없이 본문만 출력합니다.
            """;

    private static final String USER = """
            [지원자 근거]
            %s

            [희망 직무]
            %s

            [지원 맥락]
            %s

            [작성할 문항]
            "%s"

            [분량]
            - 목표: %d~%d자(공백포함), 최대 %d자.

            위 문항에 대한 자소서 본문만 출력하세요. 지원 맥락이 특정 회사·직무를 가리키면 그에 맞춰 쓰되, 근거에 없는 사실은 만들지 마세요.
            """;

    private static final String RETRY_OVER = """
            방금 글이 %d자로 상한(%d자)을 초과했습니다. 핵심은 유지하되 %d자 이하로 줄여 다시 쓰세요. 본문만.
            [직전 작성본]
            %s
            """;

    private static final String RETRY_SHORT = """
            방금 글이 %d자로 목표(%d자 이상)에 못 미쳐 빈약합니다.
            근거에 있는 사실을 더 활용해 %d~%d자로 보강하세요(없는 내용 금지). 본문만.
            [직전 작성본]
            %s
            """;

    public ResumeDraft generate(String username, String template) {
        String tpl = (template == null || template.isBlank()) ? "general" : template;
        ResumeData facts = assembler.assemble(username);
        String evidence = assembler.buildEvidence(facts);
        String targetJob = facts.targetJob() == null ? "" : facts.targetJob();

        List<CoverLetterSection> sections = new ArrayList<>();
        List<HonestyReport.Fix> allFixes = new ArrayList<>();

        for (String question : GENERAL_QUESTIONS) {
            String body = generateSection(evidence, targetJob, question, null);
            HonestyVerifier.FixResult fixed = honestyVerifier.verifyAndFix(question, body, evidence);
            allFixes.addAll(fixed.fixes());
            String finalBody = fixed.text();
            sections.add(new CoverLetterSection(
                    question, finalBody, CharCounter.count(finalBody, true)));
        }

        HonestyReport report = new HonestyReport(allFixes);
        ResumeData withCover = new ResumeData(
                facts.personal(), facts.education(), facts.targetJob(), facts.skills(), facts.projects(),
                facts.careers(), facts.certs(), facts.languages(), facts.awards(),
                sections, new Meta(tpl, null, report));

        return new ResumeDraft(withCover, report, tpl);
    }

    public ResumeDraft generateForJob(String username, String template, JobPosting job, MatchReport match) {
        String tpl = (template == null || template.isBlank()) ? "general" : template;
        ResumeData facts = assembler.assemble(username);
        String evidence = assembler.buildEvidence(facts);
        String targetJob = (job.position() == null || job.position().isBlank())
                ? (facts.targetJob() == null ? "" : facts.targetJob())
                : job.position();
        String jobContext = buildJobContext(job, match);

        List<CoverLetterSection> sections = new ArrayList<>();
        List<HonestyReport.Fix> allFixes = new ArrayList<>();
        for (String question : GENERAL_QUESTIONS) {
            String body = generateSection(evidence, targetJob, question, jobContext);
            HonestyVerifier.FixResult fixed = honestyVerifier.verifyAndFix(question, body, evidence);
            allFixes.addAll(fixed.fixes());
            String finalBody = fixed.text();
            sections.add(new CoverLetterSection(question, finalBody, CharCounter.count(finalBody, true)));
        }

        HonestyReport report = new HonestyReport(allFixes);
        ResumeData withCover = new ResumeData(
                facts.personal(), facts.education(), targetJob, facts.skills(), facts.projects(),
                facts.careers(), facts.certs(), facts.languages(), facts.awards(),
                sections, new Meta(tpl, null, report));
        return new ResumeDraft(withCover, report, tpl);
    }

    /** 공고+매칭을 프롬프트용 맥락 블록으로. 없는 스킬을 보유한 것처럼 쓰지 말라는 정직성은 SYSTEM에서 유지. */
    private String buildJobContext(JobPosting job, MatchReport match) {
        StringBuilder sb = new StringBuilder();
        sb.append("[지원 회사] ").append(orUnknown(job.company())).append('\n');
        sb.append("[지원 직무] ").append(orUnknown(job.position())).append('\n');
        if (job.requiredSkills() != null && !job.requiredSkills().isEmpty()) {
            sb.append("[요구 스킬] ").append(String.join(", ", job.requiredSkills())).append('\n');
        }
        if (match != null) {
            if (match.summary() != null && !match.summary().isBlank()) {
                sb.append("[매칭 요약] ").append(match.summary()).append('\n');
            }
            if (match.strengths() != null && !match.strengths().isEmpty()) {
                sb.append("[부각할 강점] ").append(String.join(", ", match.strengths().stream()
                        .map(s -> s.skill() + "(" + s.evidence() + ")").limit(6).toList())).append('\n');
            }
            if (match.gaps() != null && !match.gaps().isEmpty()) {
                sb.append("[정직하게 다룰 약점] ").append(String.join(", ", match.gaps().stream()
                        .map(g -> g.skill() + "[" + g.severity() + "]").limit(6).toList())).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private static String orUnknown(String s) { return (s == null || s.isBlank()) ? "(미상)" : s; }

    private String generateSection(String evidence, String targetJob, String question, String jobContext) {
        String tj = (targetJob == null || targetJob.isBlank()) ? "(미지정)" : targetJob;
        LengthVerdict t0 = CharCounter.check("", TARGET_LIMIT, LIMIT_TYPE);
        String jobBlock = (jobContext == null || jobContext.isBlank())
                ? "(특정 공고 없음 — 일반 지원용)"
                : jobContext;
        String user = USER.formatted(evidence, tj, jobBlock, question, t0.targetMin(), t0.targetMax(), TARGET_LIMIT);
        String text = aiFacadeService.ask(SYSTEM, user).trim();

        for (int i = 0; i < MAX_LEN_RETRY; i++) {
            LengthVerdict v = CharCounter.check(text, TARGET_LIMIT, LIMIT_TYPE);
            if (!v.needsRetry()) break;
            String retry = "over".equals(v.status())
                    ? RETRY_OVER.formatted(v.count(), TARGET_LIMIT, v.targetMax(), text)
                    : RETRY_SHORT.formatted(v.count(), v.targetMin(), v.targetMin(), v.targetMax(), text);
            text = aiFacadeService.ask(SYSTEM, retry).trim();
        }
        return text;
    }
}
