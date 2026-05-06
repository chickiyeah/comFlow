package com.campusflow.domain.roadmap.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.ai.service.ChromaDbService;
import com.campusflow.domain.roadmap.dto.RoadmapRequest;
import com.campusflow.domain.roadmap.dto.RoadmapResponse;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final ChromaDbService chromaDbService;
    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper;

    private static final String INTERNAL_SYSTEM_PROMPT = """
            당신은 컴퓨터정보과 학과 AI 어시스턴트입니다.
            반드시 아래 제공된 학과 자료 범위 내에서만 답변하세요.
            자료에 없는 내용은 "학과 자료에 해당 정보가 없습니다"라고 답하세요.
            응답은 반드시 JSON 형식으로만 반환하세요.
            """;

    private static final String EXTERNAL_SYSTEM_PROMPT = """
            당신은 IT 직업 진로 전문가입니다.
            2년제 컴퓨터정보과 학생(4학기 과정)을 위한 직업 로드맵을 작성해주세요.
            최신 채용 트렌드와 실무 요구사항을 반영하세요.
            응답은 반드시 JSON 형식으로만 반환하세요.
            """;

    private static final String ROADMAP_JSON_TEMPLATE = """
            다음 JSON 형식으로 응답해주세요:
            {
              "certificates": [
                {"name": "자격증명", "type": "REQUIRED|RECOMMENDED|OPTIONAL", "description": "설명"}
              ],
              "semesterPlans": [
                {"semester": 1, "focus": "핵심 주제", "tasks": ["할 일1", "할 일2"]}
              ]
            }
            직업: %s
            """;

    public RoadmapResponse generateRoadmap(RoadmapRequest request) {
        String userMessage = String.format(ROADMAP_JSON_TEMPLATE, request.jobTitle());
        String systemPrompt;

        if (request.useExternalAi()) {
            systemPrompt = EXTERNAL_SYSTEM_PROMPT;
        } else {
            List<String> docs = chromaDbService.searchRelevantDocs(request.jobTitle(), 5);
            String context = String.join("\n\n", docs);
            systemPrompt = INTERNAL_SYSTEM_PROMPT + "\n\n[학과 자료]\n" + context;
        }

        String rawJson = aiFacadeService.ask(systemPrompt, userMessage);

        try {
            // JSON 블록 추출 (마크다운 코드블록 처리)
            String json = rawJson.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, String>> certMaps = (List<Map<String, String>>) parsed.get("certificates");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> planMaps = (List<Map<String, Object>>) parsed.get("semesterPlans");

            List<RoadmapResponse.Certificate> certificates = certMaps.stream()
                    .map(m -> new RoadmapResponse.Certificate(m.get("name"), m.get("type"), m.get("description")))
                    .toList();

            List<RoadmapResponse.SemesterPlan> plans = planMaps.stream()
                    .map(m -> new RoadmapResponse.SemesterPlan(
                            ((Number) m.get("semester")).intValue(),
                            (String) m.get("focus"),
                            (List<String>) m.get("tasks")))
                    .toList();

            return new RoadmapResponse(
                    request.jobTitle(), certificates, plans,
                    request.useExternalAi() ? "EXTERNAL" : "INTERNAL"
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}
