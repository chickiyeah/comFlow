package com.campusflow.domain.chat.service;

import com.campusflow.domain.chat.dto.*;
import com.campusflow.domain.chat.entity.ChatMessage;
import com.campusflow.domain.chat.entity.ChatSession;
import com.campusflow.domain.chat.repository.ChatMessageRepository;
import com.campusflow.domain.chat.repository.ChatSessionRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.campusflow.global.service.KomjeongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final KomjeongService komjeongService;

    public List<ChatSessionResponse> listSessions(String username) {
        Long userId = resolveUserId(username);
        return sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    public ChatSessionDetailResponse getSession(String username, Long sessionId) {
        ChatSession s = loadOwnedSession(username, sessionId);
        List<ChatMessageResponse> msgs = messageRepo.findBySessionIdOrderByCreatedAtAsc(s.getId()).stream()
                .map(ChatMessageResponse::from)
                .toList();
        return new ChatSessionDetailResponse(s.getId(), s.getTitle(), msgs);
    }

    @Transactional
    public ChatSendResponse createSessionWithMessage(String username, String message) {
        Long userId = resolveUserId(username);
        String sessionKey = "cf_" + UUID.randomUUID();
        String title = makeTitle(message);
        ChatSession session = sessionRepo.save(new ChatSession(userId, sessionKey, title));
        return appendExchange(session, message);
    }

    @Transactional
    public ChatSendResponse sendMessage(String username, Long sessionId, String message) {
        ChatSession session = loadOwnedSession(username, sessionId);
        return appendExchange(session, message);
    }

    @Transactional
    public ChatSessionResponse renameSession(String username, Long sessionId, String title) {
        ChatSession s = loadOwnedSession(username, sessionId);
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (trimmed.length() > 200) trimmed = trimmed.substring(0, 200);
        s.updateTitle(trimmed);
        s.touch();
        return ChatSessionResponse.from(s);
    }

    @Transactional
    public void deleteSession(String username, Long sessionId) {
        ChatSession s = loadOwnedSession(username, sessionId);
        messageRepo.deleteBySessionId(s.getId());
        sessionRepo.delete(s);
    }

    private ChatSendResponse appendExchange(ChatSession session, String message) {
        ChatMessage userMsg = messageRepo.save(
                new ChatMessage(session.getId(), "user", message));

        String answer = komjeongService.queryWithSession(message, session.getSessionKey());
        if (answer == null || answer.isBlank()) {
            answer = "답변을 가져오지 못했습니다.";
        }

        ChatMessage botMsg = messageRepo.save(
                new ChatMessage(session.getId(), "assistant", answer));
        session.touch();

        return new ChatSendResponse(
                session.getId(),
                session.getTitle(),
                ChatMessageResponse.from(userMsg),
                ChatMessageResponse.from(botMsg)
        );
    }

    private String makeTitle(String message) {
        if (message == null) return "새 채팅";
        String t = message.trim().replaceAll("\\s+", " ");
        return t.length() > 30 ? t.substring(0, 30) + "…" : t;
    }

    private Long resolveUserId(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED))
                .getId();
    }

    private ChatSession loadOwnedSession(String username, Long sessionId) {
        Long userId = resolveUserId(username);
        ChatSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!s.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return s;
    }
}
