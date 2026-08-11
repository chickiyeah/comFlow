package com.campusflow.domain.classmeeting.controller;

import com.campusflow.domain.classmeeting.dto.MeetingResponse;
import com.campusflow.domain.classmeeting.service.ClassMeetingService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes/{classId}/meeting")
@RequiredArgsConstructor
public class ClassMeetingController {

    private final ClassMeetingService meetingService;

    @PostMapping
    public ApiResponse<MeetingResponse> start(@AuthenticationPrincipal String username,
                                              @PathVariable Long classId) {
        return ApiResponse.ok(meetingService.start(username, classId));
    }

    @GetMapping
    public ApiResponse<MeetingResponse> current(@AuthenticationPrincipal String username,
                                                @PathVariable Long classId) {
        return ApiResponse.ok(meetingService.current(username, classId));
    }

    @DeleteMapping
    public ApiResponse<MeetingResponse> end(@AuthenticationPrincipal String username,
                                            @PathVariable Long classId) {
        return ApiResponse.ok(meetingService.end(username, classId));
    }
}
