package com.campusflow.domain.resume.controller;

import com.campusflow.domain.resume.dto.ResumeRequest;
import com.campusflow.domain.resume.dto.ResumeResponse;
import com.campusflow.domain.resume.service.PdfService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final PdfService pdfService;

    @GetMapping
    public ApiResponse<List<ResumeResponse>> getMyResumes(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok(resumeService.getMyResumes(user.getUsername()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeResponse> getResume(@AuthenticationPrincipal UserDetails user,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(resumeService.getResume(user.getUsername(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResumeResponse> create(@AuthenticationPrincipal UserDetails user,
                                               @Valid @RequestBody ResumeRequest request) {
        return ApiResponse.ok(resumeService.create(user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResumeResponse> update(@AuthenticationPrincipal UserDetails user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ResumeRequest request) {
        return ApiResponse.ok(resumeService.update(user.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        resumeService.delete(user.getUsername(), id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal UserDetails user,
                                               @PathVariable Long id) {
        ResumeResponse resume = resumeService.getResume(user.getUsername(), id);
        byte[] pdf = pdfService.generateResumePdf(resume);

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
