package com.campusflow.domain.resume.dto;

import com.campusflow.domain.resume.dto.ResumeData.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeDataJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 직렬화_역직렬화_왕복() throws Exception {
        ResumeData data = new ResumeData(
                new Personal("홍길동", "201918023", "hong@campus.ac", "010-1234-5678"),
                new Education("컴퓨터정보과", 2, 1, 4.05),
                "백엔드 개발자",
                List.of(new SkillGroup("언어", List.of("Java", "Python"))),
                List.of(new ProjectEntry("캠퍼스플로우", "2025.03~2025.06",
                        List.of("Spring", "React"), "백엔드", "출결 수기 관리",
                        "REST API 자동화", "처리시간 60% 단축", "https://github.com/x", null)),
                List.of(new CareerEntry("ABC", "2025.07~2025.08", "인턴", "경력", "백엔드 인턴")),
                List.of(new CertEntry("정보처리기능사", "한국산업인력공단", "2025.06")),
                List.of(new LanguageEntry("TOEIC", "800", "2025.05")),
                List.of(new AwardEntry("교내경진대회", "전주비전대", "금상", "2025.11")),
                List.of(new CoverLetterSection("성장과정", "저는...", 5)),
                new Meta("general", "2026-07-31T10:00:00", null)
        );

        String json = mapper.writeValueAsString(data);
        ResumeData back = mapper.readValue(json, ResumeData.class);

        assertThat(back.personal().name()).isEqualTo("홍길동");
        assertThat(back.education().gpa()).isEqualTo(4.05);
        assertThat(back.skills().get(0).items()).containsExactly("Java", "Python");
        assertThat(back.coverLetter().get(0).question()).isEqualTo("성장과정");
        assertThat(back.targetJob()).isEqualTo("백엔드 개발자");
    }
}
