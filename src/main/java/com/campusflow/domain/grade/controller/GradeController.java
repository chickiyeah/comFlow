package com.campusflow.domain.grade.controller;

import com.campusflow.domain.grade.dto.GradeResponse;
import com.campusflow.domain.grade.dto.GradeSummaryResponse;
import com.campusflow.domain.grade.service.GradeService;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final PdfService pdfService;

    @GetMapping("/me")
    public ApiResponse<GradeSummaryResponse> getMyGrades(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(gradeService.getMyGrades(username));
    }

    @GetMapping("/me/semester")
    public ApiResponse<List<GradeResponse>> getGradesBySemester(
            @AuthenticationPrincipal String username,
            @RequestParam int year,
            @RequestParam int semester) {
        return ApiResponse.ok(gradeService.getGradesBySemester(username, year, semester));
    }

    /** 성적증명서 PDF 다운로드 */
    @GetMapping("/transcript/pdf")
    public ResponseEntity<byte[]> transcriptPdf(@AuthenticationPrincipal String username) {
        Map<String, Object> model = gradeService.getTranscriptModel(username);
        byte[] pdf = pdfService.generateTranscriptPdf(model);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("성적증명서.pdf", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
