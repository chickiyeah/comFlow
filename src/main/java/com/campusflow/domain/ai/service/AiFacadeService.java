package com.campusflow.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OpenAI(primary) → Claude(backup) 순서로 호출.
 * OpenAI 호출 실패 시 자동으로 Claude로 폴백.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFacadeService {

    private final OpenAiService openAiService;
    private final ClaudeApiService claudeApiService;

    public String ask(String systemPrompt, String userMessage) {
        try {
            String result = openAiService.ask(systemPrompt, userMessage);
            if (!result.isBlank()) {
                return result;
            }
            log.warn("OpenAI returned empty response, falling back to Claude");
        } catch (Exception e) {
            log.warn("OpenAI API failed ({}), falling back to Claude", e.getMessage());
        }

        return claudeApiService.ask(systemPrompt, userMessage);
    }
}
