package com.campusflow.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NovaClass 포팅 AI 기능의 단일 텍스트 진입점. 함대 LiteLLM(Groq gpt-oss-120b)이 설정돼 있으면 우선 사용하고,
 * 미설정/실패/빈 응답이면 기존 {@link AiFacadeService}(jvision→ollama→gemini)로 폴백한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTextService {

    private final LiteLlmService liteLlmService;
    private final AiFacadeService aiFacadeService;

    public String ask(String systemPrompt, String userMessage) {
        if (liteLlmService.isEnabled()) {
            try {
                String result = liteLlmService.ask(systemPrompt, userMessage);
                if (result != null && !result.isBlank()) {
                    return result;
                }
                log.info("[AI] LiteLLM blank -> AiFacade fallback");
            } catch (Exception e) {
                log.warn("[AI] LiteLLM FAIL ({}) -> AiFacade fallback", e.getMessage());
            }
        }
        return aiFacadeService.ask(systemPrompt, userMessage);
    }
}
