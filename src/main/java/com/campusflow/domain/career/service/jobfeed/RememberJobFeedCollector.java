package com.campusflow.domain.career.service.jobfeed;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RememberJobFeedCollector implements JobFeedCollector {

    static final String SITEMAP_URL = "https://career-cdn.rememberapp.co.kr/upload/sitemap/job_posting.xml";
    private static final int DEFAULT_LIMIT = 30;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 Chrome/126.0 Safari/537.36";

    private final RestClient restClient;
    private final SitemapParser sitemapParser;
    private final JsonLdJobPostingParser postingParser;
    private final int limit;

    @Autowired
    public RememberJobFeedCollector(ObjectMapper objectMapper) {
        this(defaultClient(), new SitemapParser(), new JsonLdJobPostingParser(objectMapper), DEFAULT_LIMIT);
    }

    RememberJobFeedCollector(RestClient restClient, SitemapParser sitemapParser,
                             JsonLdJobPostingParser postingParser, int limit) {
        this.restClient = restClient;
        this.sitemapParser = sitemapParser;
        this.postingParser = postingParser;
        this.limit = limit;
    }

    @Override
    public String source() {
        return "리멤버";
    }

    @Override
    public List<JobSearchResult> collectLatest() {
        try {
            List<JobSearchResult> results = new ArrayList<>();
            String xml = get(SITEMAP_URL, "https://career.rememberapp.co.kr/");
            for (SitemapParser.SitemapEntry entry : sitemapParser.parse(xml)) {
                if (results.size() >= limit) break;
                try {
                    postingParser.parse(get(entry.url(), SITEMAP_URL), entry.url(), source())
                            .filter(this::isActive)
                            .ifPresent(results::add);
                } catch (Exception e) {
                    log.debug("[채용수집] 리멤버 상세 수집 실패 {}: {}", entry.url(), e.getMessage());
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("[채용수집] 리멤버 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isActive(JobSearchResult job) {
        return job.deadline() == null || !job.deadline().isBefore(LocalDate.now());
    }

    private String get(String url, String referer) {
        return restClient.get().uri(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", referer)
                .retrieve().body(String.class);
    }

    private static RestClient defaultClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(12_000);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
