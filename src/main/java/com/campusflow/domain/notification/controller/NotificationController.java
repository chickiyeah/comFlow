package com.campusflow.domain.notification.controller;

import com.campusflow.domain.jobalert.service.JobAlertService;
import com.campusflow.domain.notification.dto.NotificationResponse;
import com.campusflow.domain.notification.service.NotificationService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JobAlertService jobAlertService;

    /** 내 알림 목록 (최근순) */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(notificationService.list(username));
    }

    /** 안 읽은 알림 수 */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(username)));
    }

    /** 단건 읽음 처리 */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal String username, @PathVariable Long id) {
        notificationService.markRead(username, id);
        return ApiResponse.ok(null);
    }

    /** 전체 읽음 처리 */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal String username) {
        notificationService.markAllRead(username);
        return ApiResponse.ok(null);
    }

    /** 내 채용 알리미를 지금 즉시 실행 → 새 공고가 있으면 알림 생성 */
    @PostMapping("/refresh-jobs")
    public ApiResponse<Map<String, Integer>> refreshJobs(@AuthenticationPrincipal String username) {
        int created = jobAlertService.runForStudent(username);
        return ApiResponse.ok(Map.of("created", created));
    }

    /** 알림 수신 설정 조회 */
    @GetMapping("/prefs")
    public ApiResponse<Map<String, Boolean>> getPrefs(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(notificationService.getPref(username));
    }

    /** 알림 수신 설정 변경. body: { jobAlert, notice } */
    @PutMapping("/prefs")
    public ApiResponse<Map<String, Boolean>> updatePrefs(@AuthenticationPrincipal String username,
                                                         @RequestBody Map<String, Boolean> body) {
        return ApiResponse.ok(notificationService.updatePref(username,
                body.getOrDefault("jobAlert", true), body.getOrDefault("notice", true)));
    }
}
