package com.campusflow.domain.study.controller;

import com.campusflow.domain.study.dto.StudyGroupRequest;
import com.campusflow.domain.study.dto.StudyGroupResponse;
import com.campusflow.domain.study.service.StudyService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @GetMapping
    public ApiResponse<List<StudyGroupResponse>> search(
            @RequestParam(required = false) String subject,
            @AuthenticationPrincipal String username) {
        return ApiResponse.ok(studyService.search(subject, username));
    }

    @GetMapping("/me")
    public ApiResponse<List<StudyGroupResponse>> getMyGroups(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(studyService.getMyGroups(username));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyGroupResponse> create(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody StudyGroupRequest request) {
        return ApiResponse.ok(studyService.create(username, request));
    }

    @PostMapping("/{id}/join")
    public ApiResponse<StudyGroupResponse> join(
            @AuthenticationPrincipal String username,
            @PathVariable Long id) {
        return ApiResponse.ok(studyService.join(username, id));
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal String username, @PathVariable Long id) {
        studyService.leave(username, id);
    }
}
