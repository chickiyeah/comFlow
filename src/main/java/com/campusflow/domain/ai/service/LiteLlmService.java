package com.campusflow.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 함대(fleet) LiteLLM 게이트웨이 클라이언트 (OpenAI 호환). Groq {@code gpt-oss-120b}(텍스트) /
 * {@code whisper-large-v3}(음성)가 LiteLLM에 등록돼 있고, {@code litellm.base-url}이 설정되면 활성화된다.
 * 미설정 시 {@link #isEnabled()}가 false → 호출부는 기존 {@link AiFacadeService}로 폴백한다.
 * (NovaClass 포팅 AI 기능이 B2부터 이 서비스를 사용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiteLlmService {

    @Value("${litellm.base-url:}")
    private String baseUrl;

    @Value("${litellm.api-key:}")
    private String apiKey;

    @Value("${litellm.text-model:smart}")
    private String textModel;

    @Value("${litellm.audio-model:whisper-large-v3}")
    private String audioModel;

    @Value("${litellm.audio-enabled:false}")
    private boolean audioEnabled;

    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * 음성 전사(whisper) 사용 가능 여부. base-url 활성화와 별개로, 게이트웨이에 whisper가
     * 등록돼 {@code litellm.audio-enabled=true}일 때만 true. (게이트웨이 미등록 상태에서 text-only
     * 이관 시 음성 경로가 하드 에러로 바뀌는 회귀를 방지한다.)
     */
    public boolean isAudioEnabled() {
        return isEnabled() && audioEnabled;
    }

    /** 텍스트 채팅 완성. 미설정이면 IllegalStateException(호출부가 폴백 판단). */
    public String ask(String systemPrompt, String userMessage) {
        if (!isEnabled()) {
            throw new IllegalStateException("LiteLLM base-url 미설정");
        }
        Map<String, Object> body = Map.of(
                "model", textModel,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );
        String raw = RestClient.create(baseUrl).post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").get(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("LiteLLM 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }

    /** 음성 전사(Groq whisper-large-v3). 미설정이면 IllegalStateException. */
    public String transcribeAudio(MultipartFile file) {
        if (!isEnabled()) {
            throw new IllegalStateException("LiteLLM base-url 미설정");
        }
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
        parts.add("model", audioModel);
        parts.add("response_format", "text");
        return RestClient.create(baseUrl).post()
                .uri("/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(String.class);
    }
}
