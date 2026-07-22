package com.campusflow.domain.material.service;

import com.campusflow.domain.ai.service.AiTextService;
import com.campusflow.domain.material.dto.MaterialAiRequest;
import com.campusflow.domain.material.dto.MaterialAiResponse;
import com.campusflow.domain.material.dto.MaterialSummaryResponse;
import com.campusflow.domain.material.entity.Material;
import com.campusflow.domain.material.entity.MaterialSummary;
import com.campusflow.domain.material.repository.MaterialRepository;
import com.campusflow.domain.material.repository.MaterialSummaryRepository;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 자료별 AI 기능: 3단계 요약(캐시), 레벨별 튜터 채팅, 자료 기반 퀴즈 생성.
 * 모든 AI 호출은 {@link AiTextService}(LiteLLM→AiFacade 폴백) 경유. JSON 파싱은 QuizService의 방어적 idiom을 따름.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialAiService {

    private static final String SUMMARY_SYSTEM = """
            당신은 학습자료 요약 도우미입니다. 주어진 자료 본문을 3단계로 요약하세요.
            반드시 JSON만 출력하고 다른 설명은 쓰지 마세요:
            {"short":"한 줄 요약","paragraph":"한 단락 요약","detailed":"핵심 포인트를 정리한 상세 요약"}
            한국어로 작성하세요.
            """;

    private static final String QUIZ_SYSTEM = """
            당신은 한국 전문대 교수입니다. 주어진 학습자료 내용을 근거로 객관식 퀴즈를 출제하세요.
            반드시 JSON 배열만 출력하고 다른 설명은 쓰지 마세요. 각 문항 형식:
            {"type":"MCQ","text":"문제","options":["보기1","보기2","보기3","보기4"],"correctAnswer":"정답보기의 0부터 시작하는 인덱스(문자열)","points":5}
            한국어로 3~5문항 출제하고, 정답 인덱스는 options 범위 안의 숫자 문자열로 하세요.
            """;

    private final MaterialRepository materialRepository;
    private final MaterialSummaryRepository summaryRepository;
    private final ClassAccessService classAccess;
    private final AiTextService aiTextService;
    private final ObjectMapper objectMapper;

    /** 3단계 요약 조회(없으면 생성·캐시). */
    @Transactional
    public MaterialSummaryResponse summary(String username, Long materialId) {
        Material material = loadAsMember(username, materialId);
        MaterialSummary existing = summaryRepository.findByMaterialId(materialId).orElse(null);
        if (existing != null) {
            return MaterialSummaryResponse.from(existing);
        }
        String context = materialContext(material);
        if (context.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Map<String, Object> parsed = askJsonObject(SUMMARY_SYSTEM, context);
        MaterialSummary summary = MaterialSummary.builder()
                .material(material)
                .shortSummary(str(parsed.get("short")))
                .paragraphSummary(str(parsed.get("paragraph")))
                .detailedSummary(str(parsed.get("detailed")))
                .build();
        return MaterialSummaryResponse.from(summaryRepository.save(summary));
    }

    /** 자료별 AI 액션: chat / quiz. */
    public MaterialAiResponse action(String username, Long materialId, MaterialAiRequest request) {
        Material material = loadAsMember(username, materialId);
        String action = request.action() == null ? "" : request.action().trim().toLowerCase();
        return switch (action) {
            case "chat" -> MaterialAiResponse.chat(chat(material, request));
            case "quiz" -> MaterialAiResponse.quiz(quiz(material));
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }

    private String chat(Material material, MaterialAiRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String level = switch (request.level() == null ? "" : request.level().trim()) {
            case "초급", "beginner" -> "초급";
            case "고급", "advanced" -> "고급";
            default -> "중급";
        };
        String system = "당신은 %s 수준 학습자를 돕는 친절한 튜터입니다. 주어진 학습자료 내용을 근거로 한국어로 이해하기 쉽게 답하세요."
                .formatted(level);
        String user = materialContext(material) + "\n\n질문: " + request.message();
        return aiTextService.ask(system, user);
    }

    private Object quiz(Material material) {
        String context = materialContext(material);
        if (context.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return askJsonArray(QUIZ_SYSTEM, context);
    }

    private String materialContext(Material material) {
        StringBuilder sb = new StringBuilder();
        if (material.getTitle() != null) {
            sb.append("자료 제목: ").append(material.getTitle()).append('\n');
        }
        if (material.getInstructions() != null && !material.getInstructions().isBlank()) {
            sb.append("설명: ").append(material.getInstructions()).append('\n');
        }
        if (material.getTextContent() != null && !material.getTextContent().isBlank()) {
            sb.append("본문:\n").append(material.getTextContent());
        }
        return sb.toString().trim();
    }

    // ── AI JSON 파싱 (QuizService idiom) ───────────────────────
    private Map<String, Object> askJsonObject(String system, String user) {
        try {
            String raw = clean(aiTextService.ask(system, user));
            int s = raw.indexOf('{'), e = raw.lastIndexOf('}');
            if (s >= 0 && e > s) {
                raw = raw.substring(s, e + 1);
            }
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("[자료AI] JSON(object) 파싱 실패: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private List<Map<String, Object>> askJsonArray(String system, String user) {
        try {
            String raw = clean(aiTextService.ask(system, user));
            int s = raw.indexOf('['), e = raw.lastIndexOf(']');
            if (s >= 0 && e > s) {
                raw = raw.substring(s, e + 1);
            }
            List<Map<String, Object>> arr = objectMapper.readValue(raw, new TypeReference<>() {});
            if (arr.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }
            return arr;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.warn("[자료AI] JSON(array) 파싱 실패: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private String clean(String raw) {
        String r = raw == null ? "" : raw.trim();
        if (r.startsWith("```")) {
            r = r.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        return r;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private Material loadAsMember(String username, Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        classAccess.requireMember(material.getClassRoom().getId(), username);
        return material;
    }
}
