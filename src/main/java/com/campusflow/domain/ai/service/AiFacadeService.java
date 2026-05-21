package com.campusflow.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * AI 폴백 체인 (노션 AI 문서 기반):
 *   1순위  jvision.ai       — 전주비전대 자체 vLLM (외부 인터넷 가능)
 *   2순위  Ollama 로컬 서버  — 10.8.0.30~34 round-robin (내부망)
 *   3순위  Google Gemini    — 최후 폴백 (외부 API)
 *
 * 세맨틱 캐싱: 동일 (systemPrompt + userMessage) 조합은 30분간 LLM 호출 없이 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFacadeService {

    private final JvisionAiService jvisionAiService;
    private final OllamaService    ollamaService;
    private final GeminiService    geminiService;

    @Cacheable(value = "aiResponses")
    public String ask(String systemPrompt, String userMessage) {

        // 1. jvision.ai
        try {
            String result = jvisionAiService.ask(systemPrompt, userMessage);
            if (!result.isBlank()) {
                log.debug("[AI] jvision.ai 응답");
                return result;
            }
            log.warn("[AI] jvision.ai 빈 응답 → Ollama 폴백");
        } catch (Exception e) {
            log.warn("[AI] jvision.ai 실패 ({}) → Ollama 폴백", e.getMessage());
        }

        // 2. Ollama round-robin (10.8.0.30 ~ .34)
        try {
            String result = ollamaService.ask(systemPrompt, userMessage);
            if (!result.isBlank()) {
                log.debug("[AI] Ollama 응답");
                return result;
            }
            log.warn("[AI] Ollama 빈 응답 → Gemini 폴백");
        } catch (Exception e) {
            log.warn("[AI] Ollama 전체 실패 ({}) → Gemini 폴백", e.getMessage());
        }

        // 3. Gemini (최후 폴백)
        log.info("[AI] Gemini 호출");
        return geminiService.ask(systemPrompt, userMessage);
    }
}
