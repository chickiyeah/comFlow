package com.campusflow.domain.career.controller;

import com.campusflow.domain.career.dto.CertExamSchedule;
import com.campusflow.domain.career.dto.JobSearchResult;
import com.campusflow.domain.career.service.QNetService;
import com.campusflow.domain.career.service.WorknetService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/career/search")
@RequiredArgsConstructor
public class JobSearchController {

    private final WorknetService worknetService;
    private final QNetService qNetService;

    @GetMapping("/jobs")
    public ApiResponse<List<JobSearchResult>> searchJobs(
            @RequestParam(defaultValue = "IT") String keyword,
            @RequestParam(defaultValue = "1") int page) {
        return ApiResponse.ok(worknetService.searchJobs(keyword, page));
    }

    @GetMapping("/certs")
    public ApiResponse<List<CertExamSchedule>> searchCerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.ok(qNetService.getSchedules(keyword, targetYear));
    }
}
