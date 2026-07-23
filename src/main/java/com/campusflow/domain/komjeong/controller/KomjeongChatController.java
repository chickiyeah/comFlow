package com.campusflow.domain.komjeong.controller;

import com.campusflow.domain.ai.cache.SemanticCacheService;
import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.deptinfo.service.DeptInfoService;
import com.campusflow.global.response.ApiResponse;
import com.campusflow.global.service.KomjeongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 컴정이 일반 질의 프록시 — 랜딩 페이지 검색창에서 사용 (비로그인 허용).
 * 입시·교수진 등 학과 내부정보(DeptInfo) 매칭 시 그 자료를 근거로 AI가 직접 답하고,
 * 그 외에는 기존 컴정이 RAG로 위임한다.
 */
@RestController
@RequestMapping("/api/komjeong")
@RequiredArgsConstructor
public class KomjeongChatController {

    private final KomjeongService komjeongService;
    private final DeptInfoService deptInfoService;
    private final AiFacadeService aiFacadeService;
    private final SemanticCacheService semanticCache;

    private static final String DEPT_SYSTEM_PROMPT = """
            당신은 전주비전대학교 컴퓨터정보과 안내 도우미입니다.
            아래 [학과 내부 자료]를 근거로 정확하고 친절하게 답하세요.
            자료에 없는 내용은 추측하지 말고 학과 사무실 문의를 안내하세요.
            """;

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("query", "").trim();
        String sessionId = body.getOrDefault("sessionId", "").trim();
        if (question.isEmpty()) {
            return ApiResponse.ok(Map.of("answer", ""));
        }

        // 세션 대화(sessionId 있음)는 문맥 의존이라 시맨틱 캐시 우회. 일회성 검색만 캐시.
        String answer = sessionId.isEmpty()
                ? semanticCache.getOrCompute("komjeong", question, () -> computeAnswer(question, ""))
                : computeAnswer(question, sessionId);
        return ApiResponse.ok(Map.of("answer", answer));
    }

    /** 학과 내부정보 매칭 시 해당 자료 근거로 직접 답변, 그 외에는 컴정이 RAG로 위임. */
    private String computeAnswer(String question, String sessionId) {
        String deptContext = deptInfoService.buildContext(question);
        if (!deptContext.isBlank()) {
            return aiFacadeService.ask(
                    DEPT_SYSTEM_PROMPT + "\n\n[학과 내부 자료]\n" + deptContext,
                    question);
        }
        return sessionId.isEmpty()
                ? komjeongService.query(question)
                : komjeongService.queryWithSession(question, sessionId);
    }
}
