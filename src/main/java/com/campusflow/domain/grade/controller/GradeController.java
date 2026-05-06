package com.campusflow.domain.grade.controller;

import com.campusflow.domain.grade.dto.GradeResponse;
import com.campusflow.domain.grade.dto.GradeSummaryResponse;
import com.campusflow.domain.grade.service.GradeService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/me")
    public ApiResponse<GradeSummaryResponse> getMyGrades(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(gradeService.getMyGrades(userDetails.getUsername()));
    }

    @GetMapping("/me/semester")
    public ApiResponse<List<GradeResponse>> getGradesBySemester(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year,
            @RequestParam int semester) {
        return ApiResponse.ok(gradeService.getGradesBySemester(userDetails.getUsername(), year, semester));
    }
}
