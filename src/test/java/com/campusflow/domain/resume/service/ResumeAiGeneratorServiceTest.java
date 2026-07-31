package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
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
class ResumeAiGeneratorServiceTest {

    @Mock ResumeAssembler assembler;
    @Mock HonestyVerifier honestyVerifier;
    @Mock AiFacadeService ai;
    @InjectMocks ResumeAiGeneratorService service;

    @Test
    void general_양식으로_자소서4문항을_생성하고_정직성검증을_거친다() {
        ResumeData facts = new ResumeData(
                new ResumeData.Personal("홍길동", "201918023", "h@x", "010"),
                new ResumeData.Education("컴퓨터정보과", 2, 1, 4.0),
                List.of(new ResumeData.SkillGroup("보유 기술", List.of("Java"))),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), null);
        when(assembler.assemble("u")).thenReturn(facts);
        when(assembler.buildEvidence(facts)).thenReturn("[스킬] Java");
        when(ai.ask(anyString(), anyString())).thenReturn("근거 기반 자기소개 본문입니다.");
        when(honestyVerifier.verifyAndFix(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> new HonestyVerifier.FixResult(inv.getArgument(1), List.of()));

        ResumeDraft draft = service.generate("u", "general");

        assertThat(draft.template()).isEqualTo("general");
        assertThat(draft.data().coverLetter()).hasSize(4);
        assertThat(draft.data().coverLetter())
                .extracting(ResumeData.CoverLetterSection::question)
                .containsExactly("성장과정", "성격의 장단점", "지원동기", "입사 후 포부");
        assertThat(draft.data().meta().honestyReport()).isNotNull();
    }
}
