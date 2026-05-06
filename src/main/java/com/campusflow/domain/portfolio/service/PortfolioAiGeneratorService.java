package com.campusflow.domain.portfolio.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.portfolio.dto.PortfolioAiDraft;
import com.campusflow.domain.portfolio.entity.PortfolioStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioAiGeneratorService {

    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 IT 포트폴리오 작성 전문가입니다.
            주어진 프로젝트 정보를 분석하여 이력서용 포트폴리오 항목을 생성해주세요.
            반드시 아래 JSON 형식으로만 응답하세요. 마크다운 코드블록 없이 JSON만 출력하세요.

            {
              "title": "프로젝트 제목 (간결하게)",
              "description": "프로젝트 설명 (3~5문장, 핵심 기능과 성과 위주)",
              "role": "본인 역할 (예: 풀스택 개발, 백엔드 개발, 팀장)",
              "techStack": "기술스택 (쉼표 구분, 예: Java, Spring Boot, MySQL)",
              "startDate": "YYYY-MM-DD 또는 null",
              "endDate": "YYYY-MM-DD 또는 null",
              "status": "COMPLETED 또는 IN_PROGRESS"
            }
            """;

    public PortfolioAiDraft generateFromContext(String context, String source, String githubUrl) {
        String userMessage = "다음 프로젝트 정보를 분석하여 포트폴리오를 작성해주세요:\n\n" + context;
        String raw = aiFacadeService.ask(SYSTEM_PROMPT, userMessage);

        return parseAiResponse(raw, source, githubUrl);
    }

    private PortfolioAiDraft parseAiResponse(String raw, String source, String githubUrl) {
        try {
            String json = raw.trim()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            String techStackRaw = (String) map.getOrDefault("techStack", "");
            List<String> techStack = Arrays.stream(techStackRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            LocalDate startDate = parseDate((String) map.get("startDate"));
            LocalDate endDate = parseDate((String) map.get("endDate"));

            String statusRaw = (String) map.getOrDefault("status", "COMPLETED");
            PortfolioStatus status;
            try {
                status = PortfolioStatus.valueOf(statusRaw);
            } catch (IllegalArgumentException e) {
                status = PortfolioStatus.COMPLETED;
            }

            return new PortfolioAiDraft(
                    (String) map.getOrDefault("title", ""),
                    (String) map.getOrDefault("description", ""),
                    (String) map.getOrDefault("role", ""),
                    techStack,
                    startDate,
                    endDate,
                    githubUrl,
                    null,
                    status,
                    source
            );
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패, 빈 초안 반환: {}", e.getMessage());
            return new PortfolioAiDraft("", raw, "", List.of(),
                    null, null, githubUrl, null, PortfolioStatus.COMPLETED, source);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }
}
