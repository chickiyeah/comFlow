package com.campusflow.domain.resume.controller;

import com.campusflow.domain.resume.dto.ResumeRequest;
import com.campusflow.domain.resume.dto.ResumeResponse;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.domain.resume.service.ResumeAiGeneratorService;
import com.campusflow.domain.resume.service.ResumeService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final PdfService pdfService;
    private final ResumeAiGeneratorService generatorService;

    @PostMapping("/generate")
    public ApiResponse<com.campusflow.domain.resume.dto.ResumeDraft> generate(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false, defaultValue = "general") String template) {
        return ApiResponse.ok(generatorService.generate(username, template));
    }

    @GetMapping
    public ApiResponse<List<ResumeResponse>> getMyResumes(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(resumeService.getMyResumes(username));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeResponse> getResume(@AuthenticationPrincipal String username,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(resumeService.getResume(username, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResumeResponse> create(@AuthenticationPrincipal String username,
                                               @Valid @RequestBody ResumeRequest request) {
        return ApiResponse.ok(resumeService.create(username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResumeResponse> update(@AuthenticationPrincipal String username,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ResumeRequest request) {
        return ApiResponse.ok(resumeService.update(username, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        resumeService.delete(username, id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal String username,
                                              @PathVariable Long id,
                                              @RequestParam(required = false) String template) {
        ResumeResponse resume = resumeService.getResume(username, id);
        com.campusflow.domain.resume.dto.ResumeData data = null;
        if (resume.resumeData() != null && !resume.resumeData().isBlank()) {
            try {
                data = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(resume.resumeData(), com.campusflow.domain.resume.dto.ResumeData.class);
            } catch (Exception e) { data = null; } // 파싱 실패 시 구 템플릿 폴백
        }
        String tpl = (template != null && !template.isBlank()) ? template : resume.template();
        byte[] pdf = pdfService.generateResumePdf(resume, data, tpl);

        String filename = resume.title().replaceAll("[^a-zA-Z0-9가-힣]", "_") + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
        );
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
