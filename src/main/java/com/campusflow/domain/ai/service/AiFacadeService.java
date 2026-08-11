package com.campusflow.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * CampusFlow AI 텍스트 진입점. 노션 「AI 네이티브 인프라」 규칙에 따라 모든 호출을
 * LiteLLM 게이트웨이({@code 10.8.0.1:4000}, role {@code smart})로만 보낸다.
 * 라우팅·폴백·에스컬레이션은 게이트웨이가 중앙 통제하므로 클라이언트는 자체 폴백체인을 두지 않는다.
 *
 * <p>구 직접호출 서비스({@link JvisionAiService}·{@link OllamaService}·{@link GeminiService})는
 * 런타임 경로에서 제외됐으나, 게이트웨이 장애 시 임시 롤백용으로 파일·빈은 보존한다.
 *
 * <p>세맨틱 캐싱: 동일 {@code (systemPrompt + userMessage)} 조합은 캐시로 LLM 호출 없이 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFacadeService {

    private final LiteLlmService liteLlmService;

    @Cacheable(value = "aiResponses", unless = "#result == null || #result.isBlank()")
    public String ask(String systemPrompt, String userMessage) {
        String result = liteLlmService.ask(systemPrompt, userMessage);
        if (result == null || result.isBlank()) {
            log.warn("[AI] 게이트웨이 빈 응답");
            throw new RuntimeException("AI 게이트웨이 응답 실패 (LiteLLM 10.8.0.1:4000)");
        }
        log.info("[AI] 게이트웨이 OK");
        return result;
    }
}
