package com.campusflow.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * NovaClass 포팅 AI 기능의 단일 텍스트 진입점. {@link AiFacadeService}(LiteLLM 게이트웨이 + 캐시)로 위임한다.
 * 게이트웨이 전체 이관 전에는 LiteLLM→AiFacade 이중경로였으나, 이관 후 둘 다 게이트웨이가 되어 단일 위임으로 단순화.
 */
@Service
@RequiredArgsConstructor
public class AiTextService {

    private final AiFacadeService aiFacadeService;

    public String ask(String systemPrompt, String userMessage) {
        return aiFacadeService.ask(systemPrompt, userMessage);
    }
}
