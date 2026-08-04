package com.campusflow.domain.jobpilot;

import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto.ProjectItem;
import com.campusflow.domain.jobpilot.service.JobMatcherService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [매칭] 인수조건 (handoff §5.1):
 *  - RAG/Python 보유 프로필 → strengths 에 RAG, missing 에 RAG 없음
 *  - kubernetes 미보유 → gaps 에 kubernetes(severity 표시)
 * 규칙 기반이라 LLM/네트워크 mock 불필요.
 */
class JobMatcherServiceTest {

    private final JobMatcherService matcher = new JobMatcherService();

    private StudentProfileDto ragPythonProfile() {
        return new StudentProfileDto(
                "홍길동", "컴퓨터정보과", "백엔드 지망",
                List.of("Python", "RAG", "Docker"),
                List.of("정보처리기사"),
                List.of(),
                List.of(new ProjectItem("사내 문서 검색", "RAG 파이프라인 구축",
                        List.of("Python", "ChromaDB"), List.of("개인적으로 구축"), true)),
                List.of()
        );
    }

    @Test
    void 보유스킬은_strengths_missing에는_없음() {
        JobPosting job = new JobPosting(
                "효성ITX", "MLOps 엔지니어", "정규직", "신입·경력", "초대졸↑", "서울", "2026.07.12", null,
                List.of("Python", "RAG"), List.of(), List.of(), List.of("모델 서빙"),
                List.of(), null, List.of());

        MatchReport r = matcher.match(job, ragPythonProfile());

        assertThat(r.matchedSkills()).contains("RAG", "Python");
        assertThat(r.missingSkills()).doesNotContain("RAG");
        assertThat(r.strengths()).anyMatch(s -> s.skill().equalsIgnoreCase("RAG"));
    }

    @Test
    void 미보유_kubernetes는_must_gap() {
        JobPosting job = new JobPosting(
                "데이타솔루션", "AI 플랫폼 엔지니어", "정규직", "신입", "대졸이상", "서울", "채용 시 마감", null,
                List.of("Kubernetes", "OpenShift", "Python"), List.of(), List.of(), List.of(),
                List.of(), null, List.of());

        MatchReport r = matcher.match(job, ragPythonProfile());

        assertThat(r.missingSkills()).contains("Kubernetes", "OpenShift");
        assertThat(r.gaps()).anyMatch(g -> g.skill().equalsIgnoreCase("Kubernetes") && g.severity().equals("must"));
        // Python 은 보유 → 강점
        assertThat(r.matchedSkills()).contains("Python");
    }

    @Test
    void 우대사항_단일토큰_미보유는_preferred_gap() {
        JobPosting job = new JobPosting(
                "에티버스", "백엔드", null, null, null, null, null, null,
                List.of("Python"), List.of(),
                List.of("Redis", "메시지 큐 운영 경험이 있으면 좋습니다"),  // 'Redis'=토큰, 문장형은 제외
                List.of(), List.of(), null, List.of());

        MatchReport r = matcher.match(job, ragPythonProfile());

        assertThat(r.gaps()).anyMatch(g -> g.skill().equalsIgnoreCase("Redis") && g.severity().equals("preferred"));
        assertThat(r.gaps()).noneMatch(g -> g.skill().contains("메시지 큐 운영"));
    }
}
