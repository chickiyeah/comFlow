package com.campusflow.domain.multimodal.service;

import com.campusflow.domain.ai.service.AiTextService;
import com.campusflow.domain.ai.service.LiteLlmService;
import com.campusflow.domain.multimodal.dto.MultimodalResponse;
import com.campusflow.domain.portfolio.service.FileParserService;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 멀티모달 분석: 업로드 파일을 텍스트로 환원한 뒤 질문에 답한다.
 * - 문서(PDF/PPTX): FileParser로 본문 추출 → AI 답변/요약
 * - 음성: 함대 LiteLLM(whisper) 전사 → AI 답변 (LiteLLM 미설정 시 안내)
 * - 이미지: 비전 모델 미등록 상태 → 안내 (함대 LiteLLM 비전 등록 시 확장)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalService {

    private static final String QA_SYSTEM =
            "당신은 주어진 자료 내용을 근거로 질문에 답하는 도우미입니다. 한국어로 명확하게 답하세요.";

    private final FileParserService fileParserService;
    private final AiTextService aiTextService;
    private final LiteLlmService liteLlmService;

    public MultimodalResponse analyze(MultipartFile file, String question) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        if (name.endsWith(".pdf") || name.endsWith(".pptx")) {
            return document(file, question);
        }
        if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a")
                || name.endsWith(".webm") || name.endsWith(".ogg")) {
            return audio(file, question);
        }
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".webp") || name.endsWith(".gif")) {
            return new MultimodalResponse("image",
                    "이미지 분석은 함대 LiteLLM 비전 모델 등록 후 지원됩니다.");
        }
        return new MultimodalResponse("unsupported",
                "지원하지 않는 파일 형식입니다. (PDF/PPTX/음성 지원)");
    }

    private MultimodalResponse document(MultipartFile file, String question) {
        String text;
        try {
            text = fileParserService.extractText(file);
        } catch (Exception e) {
            log.warn("문서 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String answer = (question != null && !question.isBlank())
                ? aiTextService.ask(QA_SYSTEM, text + "\n\n질문: " + question)
                : text;
        return new MultimodalResponse("document", answer);
    }

    private MultimodalResponse audio(MultipartFile file, String question) {
        if (!liteLlmService.isAudioEnabled()) {
            return new MultimodalResponse("audio",
                    "음성 분석은 함대 LiteLLM(whisper) 등록 후 지원됩니다.");
        }
        String transcript;
        try {
            transcript = liteLlmService.transcribeAudio(file);
        } catch (Exception e) {
            log.warn("음성 전사 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        String answer = (question != null && !question.isBlank())
                ? aiTextService.ask(QA_SYSTEM, "음성 전사 내용:\n" + transcript + "\n\n질문: " + question)
                : transcript;
        return new MultimodalResponse("audio", answer);
    }
}
