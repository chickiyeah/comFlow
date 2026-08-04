package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAiGeneratorServiceJobTest {

    @Mock ResumeAssembler assembler;
    @Mock HonestyVerifier honestyVerifier;
    @Mock AiFacadeService ai;
    @InjectMocks ResumeAiGeneratorService service;

    @Test
    void 공고맥락으로_생성하면_targetJob이_공고직무이고_자소서4항목이_나온다() {
        ResumeData facts = new ResumeData(
                new ResumeData.Personal("홍길동", "201918023", "h@x", "010"),
                new ResumeData.Education("컴퓨터정보과", 2, 1, 4.0),
                "백엔드",                        // 학생 기존 희망직무
                List.of(new ResumeData.SkillGroup("보유 기술", List.of("Java"))),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), null);
        when(assembler.assemble("u")).thenReturn(facts);
        when(assembler.buildEvidence(facts)).thenReturn("[스킬] Java");
        when(ai.ask(anyString(), anyString())).thenReturn("근거 기반 지원동기 본문입니다.");
        when(honestyVerifier.verifyAndFix(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> new HonestyVerifier.FixResult(inv.getArgument(1), List.of()));

        JobPosting job = new JobPosting("네이버", "백엔드 엔지니어", "정규직", null, null, "성남",
                "2026-09-01", null, List.of("Java", "Spring"),
                List.of(), List.of(), List.of(), List.of(), null, List.of());
        MatchReport match = new MatchReport(
                List.of("Java"), List.of("Spring"),
                List.of(new MatchReport.Strength("Java", "포트폴리오")),
                List.of(new MatchReport.Gap("Spring", "must")), "Java 보유, Spring 미보유");

        ResumeDraft draft = service.generateForJob("u", "general", job, match);

        assertThat(draft.template()).isEqualTo("general");
        assertThat(draft.data().targetJob()).isEqualTo("백엔드 엔지니어");   // 공고 직무로 대체
        assertThat(draft.data().coverLetter()).hasSize(4);
        assertThat(draft.data().meta().honestyReport()).isNotNull();
    }
}
