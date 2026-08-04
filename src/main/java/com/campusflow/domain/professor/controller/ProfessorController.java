package com.campusflow.domain.professor.controller;

import com.campusflow.domain.professor.dto.ProfessorAnalyticsResponse;
import com.campusflow.domain.professor.dto.ProfessorOverviewResponse;
import com.campusflow.domain.professor.dto.ProfessorStudentDetailResponse;
import com.campusflow.domain.professor.dto.ProfessorStudentRow;
import com.campusflow.domain.professor.service.ProfessorService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 교수자 모드 — 학생 현황/성적/출결 조회.
 * 보안: SecurityConfig 에서 /api/professor/** = hasAnyRole(PROFESSOR, ADMIN).
 */
@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService professorService;

    @GetMapping("/overview")
    public ApiResponse<ProfessorOverviewResponse> overview() {
        return ApiResponse.ok(professorService.overview());
    }

    @GetMapping("/students")
    public ApiResponse<List<ProfessorStudentRow>> students() {
        return ApiResponse.ok(professorService.students());
    }

    /** 학습분석/조기경보 대시보드 */
    @GetMapping("/analytics")
    public ApiResponse<ProfessorAnalyticsResponse> analytics() {
        return ApiResponse.ok(professorService.analytics());
    }

    @GetMapping("/students/{id}")
    public ApiResponse<ProfessorStudentDetailResponse> studentDetail(@PathVariable Long id) {
        return ApiResponse.ok(professorService.detail(id));
    }

    /** 특정 학생에게 알림 발송 (인앱 + 웹푸시). body: { title?, message } */
    @PostMapping("/students/{id}/notify")
    public ApiResponse<Void> notifyStudent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        professorService.notifyStudent(id, body.get("title"), body.getOrDefault("message", ""));
        return ApiResponse.ok(null);
    }

    /** 위험군 전체에게 알림 발송. body: { title?, message } */
    @PostMapping("/notify-at-risk")
    public ApiResponse<Map<String, Integer>> notifyAtRisk(@RequestBody Map<String, String> body) {
        int n = professorService.notifyAtRisk(body.get("title"), body.getOrDefault("message", ""));
        return ApiResponse.ok(Map.of("notified", n));
    }
}
