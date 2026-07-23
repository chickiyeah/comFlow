package com.campusflow.domain.kmate.service;

import com.campusflow.domain.ai.cache.SemanticCacheService;
import com.campusflow.domain.ai.service.AiTextService;
import com.campusflow.domain.kmate.dto.*;
import com.campusflow.domain.kmate.entity.KmateHistory;
import com.campusflow.domain.kmate.repository.KmateHistoryRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * K.MATE — 외국인 유학생 TOPIK AI 튜터. 질의응답(이력 저장) + 연습 퀴즈 생성/채점.
 * 모든 AI 호출은 {@link AiTextService}(LiteLLM→AiFacade 폴백) 경유.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KmateService {

    private static final String TUTOR_SYSTEM = """
            당신은 외국인 유학생을 위한 한국어능력시험(TOPIK) 전문 AI 튜터 'K.MATE'입니다.
            한국어 문법·어휘·독해·작문 질문에 친절하고 명확하게 한국어로 답하세요.
            필요하면 쉬운 예문을 들어 설명하고, 학습자가 이해하기 쉽게 단계적으로 안내하세요.
            """;

    private static final String QUIZ_SYSTEM = """
            당신은 TOPIK 출제 전문가입니다. 주어진 주제로 한국어능력시험 스타일의 객관식 문항을 출제하세요.
            반드시 JSON 배열만 출력하고 다른 설명은 쓰지 마세요. 각 문항 형식:
            {"text":"문제","options":["보기1","보기2","보기3","보기4"],"correctAnswer":"정답 인덱스(0부터,문자열)","explanation":"정답 해설"}
            정답 인덱스는 options 범위 안의 숫자 문자열로 하세요.
            """;

    private final KmateHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AiTextService aiTextService;
    private final SemanticCacheService semanticCache;
    private final ObjectMapper objectMapper;

    @Transactional
    public KmateAskResponse ask(String username, String question) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        String answer = semanticCache.getOrCompute(
                "kmate-tutor", question, () -> aiTextService.ask(TUTOR_SYSTEM, question));
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        KmateHistory saved = historyRepository.save(KmateHistory.builder()
                .user(user).question(question).answer(answer).build());
        return new KmateAskResponse(saved.getId(), answer);
    }

    public List<KmateHistoryResponse> history(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return historyRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(KmateHistoryResponse::from).toList();
    }

    public List<KmateQuizQuestion> generateQuiz(KmateQuizGenerateRequest request) {
        int count = request.count() != null ? Math.max(1, Math.min(10, request.count())) : 5;
        String language = request.language() == null || request.language().isBlank()
                ? "한국어" : request.language();
        String user = "주제: " + request.topic() + "\n문항 수: " + count + "개\n해설 언어: " + language;

        List<KmateQuizQuestion> questions = new ArrayList<>();
        try {
            String raw = clean(aiTextService.ask(QUIZ_SYSTEM, user));
            int s = raw.indexOf('['), e = raw.lastIndexOf(']');
            if (s >= 0 && e > s) {
                raw = raw.substring(s, e + 1);
            }
            List<Map<String, Object>> arr = objectMapper.readValue(raw, new TypeReference<>() {});
            for (Map<String, Object> m : arr) {
                String text = m.get("text") != null ? m.get("text").toString() : "";
                if (text.isBlank()) {
                    continue;
                }
                List<String> options = m.get("options") instanceof List<?> l
                        ? l.stream().map(String::valueOf).toList() : List.of();
                String correct = m.get("correctAnswer") != null ? m.get("correctAnswer").toString() : "0";
                String explanation = m.get("explanation") != null ? m.get("explanation").toString() : null;
                questions.add(new KmateQuizQuestion(text, options, correct, explanation));
            }
        } catch (Exception ex) {
            log.warn("[K.MATE] 퀴즈 생성 실패: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        return questions;
    }

    public KmateQuizCheckResponse checkQuiz(KmateQuizCheckRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        int score = 0;
        List<KmateQuizCheckResponse.ItemResult> results = new ArrayList<>();
        for (KmateQuizCheckRequest.Item item : request.items()) {
            boolean correct = item.correctAnswer() != null
                    && item.correctAnswer().trim().equalsIgnoreCase(
                            item.userAnswer() == null ? "" : item.userAnswer().trim());
            if (correct) {
                score++;
            }
            results.add(new KmateQuizCheckResponse.ItemResult(
                    item.question(), correct, item.correctAnswer(), item.userAnswer()));
        }
        return new KmateQuizCheckResponse(score, request.items().size(), results);
    }

    private String clean(String raw) {
        String r = raw == null ? "" : raw.trim();
        if (r.startsWith("```")) {
            r = r.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        return r;
    }
}
