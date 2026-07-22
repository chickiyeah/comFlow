package com.campusflow.domain.classroom.controller;

import com.campusflow.domain.classroom.dto.ClassCreateRequest;
import com.campusflow.domain.classroom.dto.ClassMemberResponse;
import com.campusflow.domain.classroom.dto.ClassResponse;
import com.campusflow.domain.classroom.dto.InviteRequest;
import com.campusflow.domain.classroom.dto.JoinClassRequest;
import com.campusflow.domain.classroom.service.ClassService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassResponse> create(@AuthenticationPrincipal String username,
                                             @Valid @RequestBody ClassCreateRequest request) {
        return ApiResponse.ok(classService.create(username, request));
    }

    @GetMapping
    public ApiResponse<List<ClassResponse>> myClasses(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(classService.getMyClasses(username));
    }

    @PostMapping("/join")
    public ApiResponse<ClassResponse> join(@AuthenticationPrincipal String username,
                                           @Valid @RequestBody JoinClassRequest request) {
        return ApiResponse.ok(classService.join(username, request.code()));
    }

    @GetMapping("/{classId}")
    public ApiResponse<ClassResponse> get(@AuthenticationPrincipal String username,
                                          @PathVariable Long classId) {
        return ApiResponse.ok(classService.get(username, classId));
    }

    @GetMapping("/{classId}/members")
    public ApiResponse<List<ClassMemberResponse>> members(@AuthenticationPrincipal String username,
                                                          @PathVariable Long classId) {
        return ApiResponse.ok(classService.members(username, classId));
    }

    @PostMapping("/{classId}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassMemberResponse> invite(@AuthenticationPrincipal String username,
                                                   @PathVariable Long classId,
                                                   @Valid @RequestBody InviteRequest request) {
        return ApiResponse.ok(classService.invite(username, classId, request));
    }

    @DeleteMapping("/{classId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal String username,
                             @PathVariable Long classId,
                             @PathVariable Long userId) {
        classService.removeMember(username, classId, userId);
    }
}
