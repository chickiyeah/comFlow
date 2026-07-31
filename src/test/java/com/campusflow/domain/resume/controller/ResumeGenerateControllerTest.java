package com.campusflow.domain.resume.controller;

import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.domain.resume.service.ResumeAiGeneratorService;
import com.campusflow.domain.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResumeController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class ResumeGenerateControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ResumeService resumeService;
    @MockBean PdfService pdfService;
    @MockBean ResumeAiGeneratorService generatorService;

    @Test
    void generate가_초안을_반환한다() throws Exception {
        ResumeData data = new ResumeData(
                new ResumeData.Personal("홍길동", null, null, null),
                null, "백엔드", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new ResumeData.CoverLetterSection("지원동기", "본문", 2)),
                new ResumeData.Meta("general", null, new HonestyReport(List.of())));
        when(generatorService.generate(any(), eq("general")))
                .thenReturn(new ResumeDraft(data, new HonestyReport(List.of()), "general"));

        mvc.perform(post("/api/resumes/generate").param("template", "general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.template").value("general"))
                .andExpect(jsonPath("$.data.data.coverLetter[0].question").value("지원동기"));
    }
}
