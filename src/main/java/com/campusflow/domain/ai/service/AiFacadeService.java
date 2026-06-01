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

    @Cacheable(value = "aiResponses", unless = "#result == null || #result.isBlank()")
    public String ask(String systemPrompt, String userMessage) {

        // 1. jvision.ai
        try {
            String result = jvisionAiService.ask(systemPrompt, userMessage);
            if (!result.isBlank()) {
                log.info("[AI] jvision.ai OK");
                return result;
            }
            log.info("[AI] jvision.ai blank -> Ollama fallback");
        } catch (Exception e) {
            log.warn("[AI] jvision.ai FAIL ({}) -> Ollama fallback", e.getMessage());
        }

        // 2. Ollama round-robin (10.8.0.30 ~ .34)
        try {
            String result = ollamaService.ask(systemPrompt, userMessage);
            if (!result.isBlank()) {
                log.info("[AI] Ollama OK");
                return result;
            }
            log.warn("[AI] Ollama blank -> Gemini fallback");
        } catch (Exception e) {
            log.warn("[AI] Ollama ALL FAIL ({}) -> Gemini fallback", e.getMessage());
        }

        // 3. Gemini (최후 폴백)
        log.info("[AI] Gemini 호출");
        String geminiResult = geminiService.ask(systemPrompt, userMessage);
        if (geminiResult.isBlank()) {
            throw new RuntimeException("모든 AI 서비스 응답 실패 (jvision.ai + Ollama + Gemini)");
        }
        return geminiResult;
    }
}
