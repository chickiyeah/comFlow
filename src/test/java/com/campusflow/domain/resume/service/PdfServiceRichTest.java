package com.campusflow.domain.resume.service;

import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeResponse;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PdfService의 유일한 협력자는 Thymeleaf {@link org.thymeleaf.TemplateEngine} 뿐이므로,
 * 스프링 컨텍스트 전체를 띄우지 않고 운영 앱과 동일한 SpringTemplateEngine을 직접 구성한다.
 * (@SpringBootTest는 .env DB/JWT 값이 실제 환경변수로 존재해야 부팅되어 신규 체크아웃/CI에서 실패했음)
 */
class PdfServiceRichTest {

    private PdfService newPdfService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        return new PdfService(engine);
    }

    @Test
    void 리치데이터로_general_PDF를_생성한다() {
        PdfService pdfService = newPdfService();

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

    @Test
    void 미구현_템플릿은_general로_폴백되어_PDF를_반환한다() {
        PdfService pdfService = newPdfService();

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

        byte[] pdf = pdfService.generateResumePdf(resume, data, "ncs");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(5, pdf.length))).startsWith("%PDF");
    }
}
