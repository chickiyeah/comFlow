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
            당신은 IT 취업 포트폴리오 작성 전문가입니다.
            주어진 소스 코드, README, 파일 구조를 깊이 분석해, **실제 취업용 포트폴리오에 그대로 넣을 수 있는 수준**의
            상세하고 구체적인 프로젝트 설명을 작성하세요. 한두 문장으로 얕게 쓰지 말고, 깊이 있게 작성하세요.

            분석 핵심:
            1. 어떤 사용자를 위한 무슨 서비스/제품인가, 해결하는 문제는 무엇인가 (도메인)
            2. 구체적인 주요 기능 — 실제 코드의 API·화면·핵심 로직 기반으로 3~5개
            3. 아키텍처/설계 — 레이어 구조, 인증 방식, 외부 연동, DB, 비동기/배치 등
            4. 실제 사용한 기술 스택과 선택 근거
            5. 기술적으로 인상적인 점이나 해결한 어려움 (코드에서 확인 가능한 것만)

            ⚠️ 날조 금지: 코드/README에 근거 없는 구체 수치(사용자 수, 성능 향상 %, 매출 등)는 절대 쓰지 마세요.
            기능·기술·아키텍처는 실제로 확인된 것만 서술하세요.

            description은 아래 **5개 섹션을 줄바꿈(\\n)으로 구분**해 한국어로 풍부하게 작성하세요 (전체 350~600자):
            【프로젝트 개요】 대상 사용자 + 서비스 + 해결한 문제 (2~3문장)
            【담당 역할】 코드로 유추되는 본인 기여 (1~2문장)
            【주요 기능】 구체 기능 3~5개를 "- "로 시작하는 한 줄씩
            【기술 스택 & 설계】 사용 기술·아키텍처와 그 이유 (2~3문장)
            【핵심 성과 / 트러블슈팅】 기술적으로 인상적인 점·해결한 난점 (1~2문장)

            반드시 아래 JSON 형식으로만 응답하세요. 마크다운 코드펜스 없이 순수 JSON만 출력하세요.

            {
              "title": "프로젝트 제목 (서비스명 또는 핵심 가치 중심으로 간결하게)",
              "description": "【프로젝트 개요】...\\n\\n【담당 역할】...\\n\\n【주요 기능】\\n- ...\\n- ...\\n- ...\\n\\n【기술 스택 & 설계】...\\n\\n【핵심 성과 / 트러블슈팅】...",
              "role": "코드에서 유추한 본인 역할 (예: 풀스택 개발자, 백엔드 개발자)",
              "techStack": "실제 확인된 기술스택 모두 (언어·프레임워크·DB·인프라, 쉼표 구분)",
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
