package com.campusflow.domain.profile.controller;

import com.campusflow.domain.profile.service.PortalDataService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 학교 포털 실시간 데이터 조회
 * 연동(sync) 이후 저장된 토큰+쿠키로 NMain에서 직접 데이터 조회
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalDataController {

    private final PortalDataService portalDataService;

    /** 성적 학기 목록 (수강한 모든 학기 + 학기별 GPA·학점 요약) */
    @GetMapping("/grades/terms")
    public ApiResponse<List<Map<String, Object>>> gradeTerms(
            @AuthenticationPrincipal String username) {
        return ApiResponse.ok(portalDataService.fetchGradeTerms(username));
    }

    /**
     * 학기별 성적 상세 (과목별 점수·등급)
     * year: 2019, smr: SU002001(1학기)/SU002002(2학기)
     * termMeta: gradeTerms 결과 행의 나머지 필드 (GPA_AVG 등)
     */
    @GetMapping("/grades/detail")
    public ApiResponse<List<Map<String, Object>>> gradeDetail(
            @AuthenticationPrincipal String username,
            @RequestParam String year,
            @RequestParam String smr,
            @RequestParam(required = false, defaultValue = "") String smrNm,
            @RequestParam(required = false, defaultValue = "0") String gpaAvg,
            @RequestParam(required = false, defaultValue = "0") String sumAcqPoint,
            @RequestParam(required = false, defaultValue = "0") String sumFacPoint,
            @RequestParam(required = false, defaultValue = "0") String cntAtlecSbjt,
            @RequestParam(required = false, defaultValue = "0") String cntEvlSbjt,
            @RequestParam(required = false, defaultValue = "0") String pergAvg,
            @RequestParam(required = false, defaultValue = "0") String percPnt,
            @RequestParam(required = false, defaultValue = "0") String bachWarnCnt) {
        Map<String, String> meta = Map.of(
                "smr_nm",        smrNm,
                "gpa_avg",       gpaAvg,
                "sum_acq_point", sumAcqPoint,
                "sum_fac_point", sumFacPoint,
                "cnt_atlec_sbjt",cntAtlecSbjt,
                "cnt_evl_sbjt",  cntEvlSbjt,
                "perg_avg",      pergAvg,
                "perc_pnt",      percPnt,
                "bach_warn_cnt", bachWarnCnt
        );
        return ApiResponse.ok(portalDataService.fetchGradeDetail(username, year, smr, meta));
    }

    /** 수강 시간표 (year: 2026, smr: SU002001=1학기/SU002002=2학기) */
    @GetMapping("/schedule")
    public ApiResponse<List<Map<String, Object>>> schedule(
            @AuthenticationPrincipal String username,
            @RequestParam(defaultValue = "2026") String year,
            @RequestParam(defaultValue = "SU002001") String smr) {
        return ApiResponse.ok(portalDataService.fetchSchedule(username, year, smr));
    }

    /**
     * 통합 출결 조회 (check.jvision + LMS 병합)
     * is_interlock=0 → check.jvision, =1 → LMS
     * schoolPassword: 저장하지 않음, 요청 시에만 사용
     */
    @PostMapping("/attendance")
    public ApiResponse<Map<String, Object>> attendance(
            @AuthenticationPrincipal String username,
            @RequestBody AttendanceRequest req) {
        return ApiResponse.ok(portalDataService.fetchAttendanceCombined(
                username, req.schoolPassword(), req.year(), req.term()));
    }

    record AttendanceRequest(
            String schoolPassword,
            @com.fasterxml.jackson.annotation.JsonProperty("year") String year,
            @com.fasterxml.jackson.annotation.JsonProperty("term") String term) {
        AttendanceRequest {
            if (year  == null) year  = "2026";
            if (term  == null) term  = "1";
        }
    }

    /** 통학버스 노선·정류장·시간표 (3개 데이터셋 일괄 반환) */
    @GetMapping("/shuttle")
    public ApiResponse<Map<String, Object>> shuttle(
            @AuthenticationPrincipal String username) {
        return ApiResponse.ok(portalDataService.fetchShuttle(username));
    }
}
