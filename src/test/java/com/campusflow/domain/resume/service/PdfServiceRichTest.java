package com.campusflow.domain.resume.service;

import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PdfServiceRichTest {

    @Autowired PdfService pdfService;

    @Test
    void 리치데이터로_general_PDF를_생성한다() {
        ResumeData data = new ResumeData(
                new ResumeData.Personal("홍길동", "201918023", "h@x", "010-0000-0000"),
                new ResumeData.Education("컴퓨터정보과", 2, 1, 4.05),
                "백엔드 개발자",
                List.of(new ResumeData.SkillGroup("보유 기술", List.of("Java", "Spring"))),
                List.of(new ResumeData.ProjectEntry("캠퍼스플로우", "2025", List.of("Spring"),
                        "백엔드", null, null, "학과 관리 시스템", "https://github.com/x", null)),
                List.of(), List.of(new ResumeData.CertEntry("정보처리기능사", "산인공", "2025")),
                List.of(), List.of(),
                List.of(new ResumeData.CoverLetterSection("지원동기", "본문입니다.", 5)),
                new ResumeData.Meta("general", null, null));
        ResumeResponse resume = new ResumeResponse(1L, "제목", "요약", List.of("Java"),
                "백엔드", null, "general", List.of(), null);

        byte[] pdf = pdfService.generateResumePdf(resume, data, "general");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(5, pdf.length))).startsWith("%PDF");
    }
}
