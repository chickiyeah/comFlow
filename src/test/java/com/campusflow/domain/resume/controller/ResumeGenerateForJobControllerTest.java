package com.campusflow.domain.resume.controller;

import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.JobTailoredResumeDraft;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import com.campusflow.domain.resume.service.JobResumeService;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.domain.resume.service.ResumeAiGeneratorService;
import com.campusflow.domain.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResumeController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class ResumeGenerateForJobControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ResumeService resumeService;
    @MockBean PdfService pdfService;
    @MockBean ResumeAiGeneratorService generatorService;
    @MockBean JobResumeService jobResumeService;

    @Test
    void generateForJob가_맞춤초안을_반환한다() throws Exception {
        ResumeData data = new ResumeData(null, null, "백엔드 엔지니어", List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new ResumeData.CoverLetterSection("지원동기", "본문", 2)),
                new ResumeData.Meta("general", null, new HonestyReport(List.of())));
        JobTailoredResumeDraft out = new JobTailoredResumeDraft(
                new ResumeDraft(data, new HonestyReport(List.of()), "general"),
                new MatchReport(List.of("Java"), List.of("Spring"), List.of(), List.of(), "요약"),
                "네이버", "백엔드 엔지니어");
        when(jobResumeService.generateForJob(any(), eq("saved"), eq(5L), eq("general"))).thenReturn(out);

        mvc.perform(post("/api/resumes/generate-for-job")
                        .param("jobType", "saved").param("jobId", "5").param("template", "general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company").value("네이버"))
                .andExpect(jsonPath("$.data.position").value("백엔드 엔지니어"))
                .andExpect(jsonPath("$.data.matchReport.matchedSkills[0]").value("Java"))
                .andExpect(jsonPath("$.data.draft.template").value("general"));
    }
}
