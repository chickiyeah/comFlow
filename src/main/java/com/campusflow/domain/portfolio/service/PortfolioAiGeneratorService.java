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
            당신은 소프트웨어 엔지니어링 포트폴리오 분석 전문가입니다.
            주어진 소스 코드, README, 파일 구조를 분석하여 이 프로젝트가 **어떤 서비스/제품을 만든 것인지**
            정확하게 이해하고 이력서용 포트폴리오를 작성해주세요.

            분석 시 다음에 집중하세요:
            1. 핵심 도메인 / 해결하는 문제가 무엇인가
            2. 주요 기능이 무엇인가 (API 엔드포인트, 화면, 핵심 로직)
            3. 어떤 아키텍처 패턴을 사용했는가
            4. 실제 코드에서 확인되는 기술스택

            description은 "이 프로젝트는 ~를 위한 서비스입니다"로 시작하여
            어떤 문제를 어떻게 해결했는지 구체적으로 설명하세요 (3~5문장).

            반드시 아래 JSON 형식으로만 응답하세요. 마크다운 없이 JSON만 출력하세요.

            {
              "title": "프로젝트 제목 (서비스명 또는 핵심 기능 중심으로 간결하게)",
              "description": "이 프로젝트는 [대상 사용자]를 위한 [서비스 종류]입니다. [핵심 기능 2~3가지]. [사용한 주요 기술과 아키텍처]. [특이사항이나 성과].",
              "role": "코드에서 유추한 본인 역할 (예: 풀스택 개발자, 백엔드 개발자, 프론트엔드 개발자)",
              "techStack": "코드에서 실제 확인된 기술스택 (쉼표 구분)",
              "startDate": "YYYY-MM-DD 또는 null",
              "endDate": "YYYY-MM-DD 또는 null",
              "status": "COMPLETED 또는 IN_PROGRESS"
            }
            """;

    public PortfolioAiDraft generateFromContext(String context, String source, String githubUrl) {
        String userMessage = """
                다음 프로젝트의 소스 코드와 정보를 분석하여 이 프로젝트가 어떤 서비스를 만든 것인지 파악하고
                포트폴리오를 작성해주세요. 코드에서 실제로 확인되는 내용을 바탕으로 작성하세요:

                """ + context;
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
