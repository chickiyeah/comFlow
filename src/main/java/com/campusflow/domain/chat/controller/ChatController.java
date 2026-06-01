package com.campusflow.domain.chat.controller;

import com.campusflow.domain.chat.dto.ChatSendResponse;
import com.campusflow.domain.chat.dto.ChatSessionDetailResponse;
import com.campusflow.domain.chat.dto.ChatSessionResponse;
import com.campusflow.domain.chat.service.ChatService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 내 채팅 세션 목록 */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponse>> list(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(chatService.listSessions(username));
    }

    /** 새 채팅 시작 — 첫 메시지 포함 */
    @PostMapping("/sessions")
    public ApiResponse<ChatSendResponse> create(
            @AuthenticationPrincipal String username,
            @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        return ApiResponse.ok(chatService.createSessionWithMessage(username, message));
    }

    /** 세션 전체 (제목 + 메시지 목록) */
    @GetMapping("/sessions/{id}")
    public ApiResponse<ChatSessionDetailResponse> detail(
            @AuthenticationPrincipal String username,
            @PathVariable Long id) {
        return ApiResponse.ok(chatService.getSession(username, id));
    }

    /** 기존 세션에 메시지 추가 */
    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<ChatSendResponse> send(
            @AuthenticationPrincipal String username,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        return ApiResponse.ok(chatService.sendMessage(username, id, message));
    }

    /** 세션 이름 수정 */
    @PatchMapping("/sessions/{id}")
    public ApiResponse<ChatSessionResponse> rename(
            @AuthenticationPrincipal String username,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        return ApiResponse.ok(chatService.renameSession(username, id, title));
    }

    /** 세션 삭제 */
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal String username,
            @PathVariable Long id) {
        chatService.deleteSession(username, id);
        return ApiResponse.ok(null);
    }
}
