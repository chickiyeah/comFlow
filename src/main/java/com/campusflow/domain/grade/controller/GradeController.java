package com.campusflow.domain.grade.controller;

import com.campusflow.domain.grade.dto.GradeResponse;
import com.campusflow.domain.grade.dto.GradeSummaryResponse;
import com.campusflow.domain.grade.service.GradeService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

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
}
