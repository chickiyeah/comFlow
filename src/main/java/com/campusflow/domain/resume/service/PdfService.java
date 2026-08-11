package com.campusflow.domain.resume.service;

import com.campusflow.domain.resume.dto.ResumeResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;

@Slf4j
@Service
public class PdfService {

    private final TemplateEngine templateEngine;

    // Windows(운영 10.8.0.29 / 로컬 모두) 시스템 한글 폰트 — PDF 한글 렌더링용
    private static final String[] KOREAN_FONTS = {
            "C:/Windows/Fonts/malgun.ttf",
            "C:/Windows/Fonts/malgunbd.ttf",
            "C:/Windows/Fonts/gulim.ttc"
    };

    // 실제 구현된 리치 템플릿 화이트리스트 — 미구현 값(ncs/dev/startup/english/internship 등)은 general로 폴백(500 방지)
    private static final java.util.Set<String> IMPLEMENTED_TEMPLATES = java.util.Set.of("general");

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateResumePdf(ResumeResponse resume) {
        Context ctx = new Context();
        ctx.setVariable("resume", resume);
        return render("resume-pdf", ctx);
    }

    public byte[] generateResumePdf(ResumeResponse resume,
                                    com.campusflow.domain.resume.dto.ResumeData data,
                                    String template) {
        if (data == null) return generateResumePdf(resume);   // 구 이력서 폴백
        String tpl = (template == null || template.isBlank()) ? "general" : template;
        if (!IMPLEMENTED_TEMPLATES.contains(tpl)) tpl = "general";   // 미구현 양식 → general 폴백 (500 방지)
        Context ctx = new Context();
        ctx.setVariable("resume", resume);
        ctx.setVariable("data", data);
        return render("resume-" + tpl, ctx);
    }

    public byte[] generateTranscriptPdf(Map<String, Object> model) {
        Context ctx = new Context();
        model.forEach(ctx::setVariable);
        return render("transcript-pdf", ctx);
    }

    /** 강좌 수료증 PDF */
    public byte[] generateCertificatePdf(Map<String, Object> model) {
        Context ctx = new Context();
        model.forEach(ctx::setVariable);
        return render("certificate-pdf", ctx);
    }

    private byte[] render(String template, Context ctx) {
        String html = templateEngine.process(template, ctx);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerKoreanFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    /** 시스템 한글 폰트를 'Malgun Gothic'으로 등록 (없으면 건너뜀 — 라틴은 기본 폰트). */
    private void registerKoreanFont(PdfRendererBuilder builder) {
        for (String path : KOREAN_FONTS) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    builder.useFont(f, "Malgun Gothic");
                    return;
                } catch (Exception e) {
                    log.warn("[PDF] 한글 폰트 등록 실패 {}: {}", path, e.getMessage());
                }
            }
        }
        log.warn("[PDF] 시스템 한글 폰트를 찾지 못함 — 한글이 깨질 수 있음");
    }
}
