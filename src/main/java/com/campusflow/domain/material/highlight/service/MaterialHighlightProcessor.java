package com.campusflow.domain.material.highlight.service;

import com.campusflow.domain.ai.service.AiTextService;
import com.campusflow.domain.material.highlight.entity.MaterialHighlightAnalysis;
import com.campusflow.domain.material.highlight.entity.MaterialHighlightPage;
import com.campusflow.domain.material.highlight.entity.MaterialPdfHighlight;
import com.campusflow.domain.material.highlight.repository.MaterialHighlightAnalysisRepository;
import com.campusflow.domain.material.highlight.repository.MaterialHighlightPageRepository;
import com.campusflow.domain.material.highlight.repository.MaterialPdfHighlightRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 하이라이트 비동기 처리기. 페이지별로 AI에 핵심 용어를 요청해 저장한다. 페이지 실패는 건너뛰고(best-effort),
 * 전체 실패 시 FAILED로 마킹. 별도 빈으로 분리해 @Async + @Transactional 프록시가 정상 적용되게 한다.
 * (외부 호출 지점: {@link MaterialHighlightService}가 afterCommit에서 호출)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialHighlightProcessor {

    private static final String KEY_TERMS_SYSTEM = """
            당신은 학습자료에서 핵심 용어·구절을 뽑아 설명하는 도우미입니다.
            주어진 페이지 본문에서 중요한 항목을 최대 5개 골라 JSON 배열만 출력하세요. 형식:
            [{"excerpt":"원문 핵심 구절","explanation":"쉬운 설명","category":"용어|개념|정의"}]
            한국어로 작성하고, 다른 설명은 쓰지 마세요.
            """;

    private final MaterialHighlightAnalysisRepository analysisRepository;
    private final MaterialHighlightPageRepository pageRepository;
    private final MaterialPdfHighlightRepository highlightRepository;
    private final AiTextService aiTextService;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void process(Long analysisId) {
        MaterialHighlightAnalysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            return;
        }
        try {
            analysis.markProcessing();
            List<MaterialHighlightPage> pages = pageRepository.findByAnalysisIdOrderByPageNumberAsc(analysisId);
            int order = 0;
            for (MaterialHighlightPage page : pages) {
                order = processPage(analysis, page, order);
                analysis.incrementCompleted();
            }
            analysis.markReady();
            log.info("[하이라이트] 분석 {} 완료 ({} 페이지)", analysisId, pages.size());
        } catch (Exception e) {
            log.warn("[하이라이트] 분석 {} 실패: {}", analysisId, e.getMessage());
            analysis.markFailed(e.getMessage());
        }
    }

    private int processPage(MaterialHighlightAnalysis analysis, MaterialHighlightPage page, int order) {
        if (page.getSourceText() == null || page.getSourceText().isBlank()) {
            return order;
        }
        try {
            String raw = clean(aiTextService.ask(KEY_TERMS_SYSTEM, page.getSourceText()));
            int s = raw.indexOf('['), e = raw.lastIndexOf(']');
            if (s >= 0 && e > s) {
                raw = raw.substring(s, e + 1);
            }
            page.setCandidatesJson(raw);
            List<Map<String, Object>> items = objectMapper.readValue(raw, new TypeReference<>() {});
            for (Map<String, Object> item : items) {
                String excerpt = str(item.get("excerpt"));
                if (excerpt == null || excerpt.isBlank()) {
                    continue;
                }
                highlightRepository.save(MaterialPdfHighlight.builder()
                        .analysis(analysis)
                        .pageNumber(page.getPageNumber())
                        .excerpt(excerpt)
                        .explanation(str(item.get("explanation")))
                        .category(str(item.get("category")))
                        .rectsJson(null) // 텍스트 전용 — 좌표 없음
                        .displayOrder(order++)
                        .build());
            }
        } catch (Exception ex) {
            log.warn("[하이라이트] 페이지 {} 처리 실패(스킵): {}", page.getPageNumber(), ex.getMessage());
        }
        return order;
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
}
