package com.campusflow.domain.career.controller;

import com.campusflow.domain.career.dto.*;
import com.campusflow.domain.career.service.*;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/career/search")
@RequiredArgsConstructor
public class JobSearchController {

    private final WorknetService worknetService;
    private final JobkoreaService jobkoreaService;
    private final Work24ScraperService work24ScraperService;
    private final SaraminService saraminService;
    private final QNetService qNetService;
    private final BlindRecruitService blindRecruitService;

    // source: all | jobkorea | work24 | worknet | saramin
    @GetMapping("/jobs")
    public ApiResponse<List<JobSearchResult>> searchJobs(
            @RequestParam(defaultValue = "IT") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String career,
            @RequestParam(defaultValue = "") String empType) {
        List<JobSearchResult> results = new ArrayList<>();
        switch (source) {
            case "worknet"  -> results.addAll(worknetService.searchJobs(keyword, page + 1));
            case "jobkorea" -> results.addAll(jobkoreaService.searchJobs(keyword, page, region, career, empType));
            case "work24"   -> results.addAll(work24ScraperService.searchJobs(keyword, region, career, empType, page + 1));
            case "saramin"  -> results.addAll(saraminService.searchJobs(keyword, page, region, career, empType));
            default -> {
                results.addAll(jobkoreaService.searchJobs(keyword, page, region, career, empType));
                results.addAll(work24ScraperService.searchJobs(keyword, region, career, empType, page + 1));
                results.addAll(saraminService.searchJobs(keyword, page, region, career, empType));
                results.addAll(worknetService.searchJobs(keyword, page + 1));
            }
        }
        return ApiResponse.ok(results);
    }

    @GetMapping("/certs/schedules")
    public ApiResponse<List<CertExamSchedule>> getSchedules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(qNetService.getSchedules(keyword, year != null ? year : LocalDate.now().getYear()));
    }

    @GetMapping("/certs/list")
    public ApiResponse<List<QualificationItem>> searchQualifications(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(qNetService.searchQualifications(keyword));
    }

    @GetMapping("/certs/detail")
    public ApiResponse<List<QualificationDetail>> getQualificationDetail(
            @RequestParam(required = false) String jmCd,
            @RequestParam(required = false) String qualgbCd) {
        return ApiResponse.ok(qNetService.getQualificationDetail(jmCd, qualgbCd));
    }

    @GetMapping("/certs/locations")
    public ApiResponse<List<ExamLocation>> getExamLocations(
            @RequestParam(required = false) String brchCd) {
        return ApiResponse.ok(qNetService.getExamLocations(brchCd));
    }

    @GetMapping("/blind-recruit")
    public ApiResponse<List<BlindRecruitCompany>> searchBlindRecruit(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page) {
        return ApiResponse.ok(blindRecruitService.search(keyword, page));
    }
}
