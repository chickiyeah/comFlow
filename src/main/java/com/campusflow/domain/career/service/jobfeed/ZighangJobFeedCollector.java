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
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ZighangJobFeedCollector implements JobFeedCollector {

    static final String SITEMAP_INDEX = "https://zighang.com/seo/sitemap/sitemap-index.xml";
    private static final String RECRUITMENT_SITEMAP = "sitemap-recruitment-";
    private static final int DEFAULT_LIMIT = 30;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; CampusFlowJobCollector/1.0)";

    private final RestClient restClient;
    private final SitemapParser sitemapParser;
    private final JsonLdJobPostingParser postingParser;
    private final int limit;

    @Autowired
    public ZighangJobFeedCollector(ObjectMapper objectMapper) {
        this(defaultClient(), new SitemapParser(), new JsonLdJobPostingParser(objectMapper), DEFAULT_LIMIT);
    }

    ZighangJobFeedCollector(RestClient restClient, SitemapParser sitemapParser,
                            JsonLdJobPostingParser postingParser, int limit) {
        this.restClient = restClient;
        this.sitemapParser = sitemapParser;
        this.postingParser = postingParser;
        this.limit = limit;
    }

    @Override
    public String source() {
        return "직행";
    }

    @Override
    public List<JobSearchResult> collectLatest() {
        try {
            String indexXml = get(SITEMAP_INDEX, "https://zighang.com/");
            String latestSitemap = sitemapParser.parse(indexXml).stream()
                    .map(SitemapParser.SitemapEntry::url)
                    .filter(url -> url.contains(RECRUITMENT_SITEMAP))
                    .max(Comparator.comparingInt(this::sitemapNumber))
                    .orElse(null);
            if (latestSitemap == null) return List.of();

            List<JobSearchResult> results = new ArrayList<>();
            for (SitemapParser.SitemapEntry entry : sitemapParser.parse(get(latestSitemap, SITEMAP_INDEX))) {
                if (results.size() >= limit) break;
                try {
                    postingParser.parse(get(entry.url(), latestSitemap), entry.url(), source())
                            .filter(this::isActive)
                            .ifPresent(results::add);
                } catch (Exception e) {
                    log.debug("[채용수집] 직행 상세 수집 실패 {}: {}", entry.url(), e.getMessage());
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("[채용수집] 직행 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isActive(JobSearchResult job) {
        return job.deadline() == null || !job.deadline().isBefore(LocalDate.now());
    }

    private int sitemapNumber(String url) {
        int start = url.lastIndexOf(RECRUITMENT_SITEMAP);
        if (start < 0) return -1;
        start += RECRUITMENT_SITEMAP.length();
        int end = url.indexOf(".xml", start);
        if (end < 0) return -1;
        try {
            return Integer.parseInt(url.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1;
        }
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
