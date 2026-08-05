package com.campusflow.domain.career.service;

import com.campusflow.domain.career.entity.ImportedJob;
import com.campusflow.domain.career.repository.ImportedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 새로 수집된 채용공고 중 희망직무(AI/개발 등) 연관 키워드에 매칭되는 건을
 * Discord 웹훅으로 요약 전송한다.
 *
 * 웹훅 URL은 비밀값 — .env 의 DISCORD_WEBHOOK_URL 로만 주입되며 코드/설정파일에는
 * 절대 하드코딩하지 않는다(빈 기본값이면 전송을 조용히 스킵).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordNotifierService {

    private final ImportedJobRepository importedJobRepository;

    @Value("${discord.webhook-url:}")
    private String webhookUrl;

    @Value("${discord.alert-keywords:AI,인공지능,머신러닝,ML,딥러닝,LLM,데이터,파이썬,백엔드,개발}")
    private String alertKeywordsCsv;

    private static final int MAX_JOBS_PER_MESSAGE = 10;
    private static final int TRUNCATE_AT = 1900; // Discord 2000자 제한 대비 여유

    /** 스케줄러가 신규 수집한 공고 목록을 넘기면, 희망직무 키워드 매칭 건만 요약 전송. */
    public void notifyNewJobs(List<ImportedJob> jobs) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        if (jobs == null || jobs.isEmpty()) return;

        List<ImportedJob> matched = filterByKeywords(jobs);
        if (matched.isEmpty()) return;

        send(buildMessage("📣 새 채용공고 " + matched.size() + "건 (희망직무 연관)", matched));
    }

    /** 알림 배선 테스트용 — 항상 뭔가 보낸다. 전송한 건수(또는 -1: 웹훅 미설정) 반환. */
    public int notifyTest() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Discord] webhook-url 미설정 — 테스트 전송 스킵");
            return -1;
        }

        List<ImportedJob> active = importedJobRepository.search(LocalDate.now(), "");
        List<ImportedJob> matched = filterByKeywords(active);
        List<ImportedJob> toSend = matched.isEmpty()
                ? active.stream().limit(5).toList()
                : matched;

        send(buildMessage("🧪 [테스트] 채용 알림 — 매칭 " + toSend.size() + "건", toSend));
        return toSend.size();
    }

    /** CSV 키워드로 제목/키워드 필드를 부분일치 필터링(대소문자 무시). 매칭분 없으면 원본(최대 10건). */
    List<ImportedJob> filterByKeywords(List<ImportedJob> jobs) {
        if (jobs == null) return List.of();

        List<String> keywords = parseKeywords();
        if (keywords.isEmpty()) {
            return jobs.stream().limit(MAX_JOBS_PER_MESSAGE).toList();
        }

        List<ImportedJob> matched = new ArrayList<>();
        for (ImportedJob job : jobs) {
            String haystack = (nz(job.getTitle()) + " " + nz(job.getKeyword())).toLowerCase(Locale.ROOT);
            boolean hit = keywords.stream().anyMatch(haystack::contains);
            if (hit) {
                matched.add(job);
                if (matched.size() >= MAX_JOBS_PER_MESSAGE) break;
            }
        }
        return matched;
    }

    private List<String> parseKeywords() {
        if (alertKeywordsCsv == null || alertKeywordsCsv.isBlank()) return List.of();
        return Arrays.stream(alertKeywordsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    private String buildMessage(String header, List<ImportedJob> jobs) {
        StringBuilder sb = new StringBuilder(header).append("\n");
        for (ImportedJob job : jobs) {
            sb.append("• **").append(nz(job.getCompany())).append("** — ").append(nz(job.getTitle()));
            LocalDate deadline = job.getDeadline();
            if (deadline != null) {
                sb.append(" (~").append(deadline).append(")");
            }
            String url = job.getUrl();
            if (url != null && !url.isBlank()) {
                sb.append("\n  ").append(url);
            }
            sb.append("\n");
        }
        String content = sb.toString();
        if (content.length() > TRUNCATE_AT) {
            content = content.substring(0, TRUNCATE_AT) + "…";
        }
        return content;
    }

    private void send(String content) {
        try {
            RestClient.create()
                    .post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(Map.of("content", content))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Discord] 채용 알림 전송 OK");
        } catch (Exception e) {
            log.warn("[Discord] 알림 전송 실패: {}", e.getMessage());
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
