package com.campusflow.domain.jobpilot.service;

import com.campusflow.domain.jobpilot.dto.JobPilotRequests.CollectResult;
import com.campusflow.domain.jobpilot.entity.JobPostingEntity;
import com.campusflow.domain.jobpilot.repository.JobPostingRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [수집] 모듈. 공고 원문(rawText) 확보 → [추출]로 넘긴다.
 *
 * §3.3 크롤링 ToS (내부 서비스라도 준수):
 *  - 민간 사이트는 **사용자 트리거 1건 on-demand fetch + 붙여넣기 폴백**만. 대량/백그라운드 금지.
 *  - 도메인별 레이트리밋 + 결과 캐싱. 같은 공고(url) 재요청 시 캐시 사용.
 *  - 동적 페이지로 본문을 못 얻으면 붙여넣기 폴백 안내(예외)로 처리.
 *
 * 참고: 공공 API(워크넷/고용24/Q-Net)는 기존 career 도메인 서비스가 이미 담당한다.
 * 이 모듈은 'URL 1건 + 붙여넣기' 어댑터에 집중한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobCollectorService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private static final int MIN_RAW_LEN = 80;                 // 본문으로 인정할 최소 길이
    private static final long DOMAIN_RATE_LIMIT_MS = 3_000;    // 도메인당 최소 호출 간격
    private static final long CACHE_TTL_HOURS = 6;
    private static final int TIMEOUT_MS = 8_000;
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0 Safari/537.36";

    // 도메인 → 마지막 fetch 시각(epoch ms). 레이트리밋용 (인메모리).
    private final Map<String, Long> lastFetchAt = new ConcurrentHashMap<>();

    @Transactional
    public CollectResult collect(String username, String url, String pastedText) {
        // 1) 붙여넣기 폴백 — 어떤 사이트든 막히면 사용자가 본문 직접 입력
        if (pastedText != null && !pastedText.isBlank()) {
            Student student = getStudent(username);
            persist(student, "paste", null, pastedText.trim());
            return new CollectResult("paste", null, pastedText.trim(), LocalDateTime.now().toString(), false);
        }

        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String normUrl = url.trim();
        if (!normUrl.startsWith("http://") && !normUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Student student = getStudent(username);

        // 2) 캐시 히트 — 같은 학생이 TTL 안에 같은 URL을 수집한 적 있으면 재사용
        var cached = jobPostingRepository
                .findTopByStudentIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(
                        student.getId(), normUrl, LocalDateTime.now().minusHours(CACHE_TTL_HOURS));
        if (cached.isPresent() && cached.get().getRawText() != null
                && cached.get().getRawText().length() >= MIN_RAW_LEN) {
            log.info("[JobPilot] collect 캐시 히트: {}", normUrl);
            return new CollectResult("url", normUrl, cached.get().getRawText(),
                    cached.get().getCreatedAt().toString(), true);
        }

        // 3) 도메인 레이트리밋
        String domain = domainOf(normUrl);
        Long last = lastFetchAt.get(domain);
        if (last != null && System.currentTimeMillis() - last < DOMAIN_RATE_LIMIT_MS) {
            log.warn("[JobPilot] 도메인 레이트리밋: {} — 잠시 후 재시도 또는 붙여넣기 권장", domain);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 4) on-demand fetch (정적 본문) — 실패/빈약하면 붙여넣기 폴백 안내
        String rawText = fetch(normUrl);
        lastFetchAt.put(domain, System.currentTimeMillis());

        if (rawText == null || rawText.length() < MIN_RAW_LEN) {
            log.info("[JobPilot] 본문 추출 실패/빈약(동적 페이지 가능): {} — 붙여넣기 폴백 필요", normUrl);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);  // UI: 붙여넣기 폴백 안내
        }

        persist(student, "url", normUrl, rawText);
        return new CollectResult("url", normUrl, rawText, LocalDateTime.now().toString(), false);
    }

    private String fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .get();
            doc.select("script, style, nav, footer, header, noscript").remove();
            String text = doc.body() != null ? doc.body().text() : doc.text();
            return text == null ? null : text.replaceAll("\\s{2,}", " ").trim();
        } catch (Exception e) {
            log.warn("[JobPilot] URL fetch 실패 {}: {}", url, e.getMessage());
            return null;
        }
    }

    private void persist(Student student, String source, String url, String rawText) {
        jobPostingRepository.save(JobPostingEntity.builder()
                .student(student).source(source).url(url).rawText(rawText).build());
    }

    private static String domainOf(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
