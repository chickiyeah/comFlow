package com.campusflow.domain.career.service.jobfeed;

import com.campusflow.domain.career.dto.JobSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class InThisWorkJobFeedCollector implements JobFeedCollector {

    static final String SITEMAP_INDEX = "https://inthiswork.com/sitemap-index-1.xml";
    private static final int DEFAULT_LIMIT = 30;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; CampusFlowJobCollector/1.0)";
    private static final Pattern DATE = Pattern.compile("(20\\d{2})[.-](\\d{1,2})[.-](\\d{1,2})");

    private final RestClient restClient;
    private final SitemapParser sitemapParser;
    private final int limit;

    @Autowired
    public InThisWorkJobFeedCollector() {
        this(defaultClient(), new SitemapParser(), DEFAULT_LIMIT);
    }

    InThisWorkJobFeedCollector(RestClient restClient, SitemapParser sitemapParser, int limit) {
        this.restClient = restClient;
        this.sitemapParser = sitemapParser;
        this.limit = limit;
    }

    @Override
    public String source() {
        return "인디스워크";
    }

    @Override
    public List<JobSearchResult> collectLatest() {
        try {
            String indexXml = get(SITEMAP_INDEX, "https://inthiswork.com/");
            String latestSitemap = sitemapParser.parse(indexXml).stream()
                    .map(SitemapParser.SitemapEntry::url)
                    .filter(url -> url.matches("https://inthiswork\\.com/sitemap-\\d+\\.xml"))
                    .findFirst()
                    .orElse(null);
            if (latestSitemap == null) return List.of();

            List<JobSearchResult> results = new ArrayList<>();
            int attempted = 0;
            for (SitemapParser.SitemapEntry entry : sitemapParser.parse(get(latestSitemap, SITEMAP_INDEX))) {
                if (attempted++ >= limit) break;
                try {
                    parseArticle(get(entry.url(), latestSitemap), entry.url()).ifPresent(results::add);
                } catch (Exception e) {
                    log.debug("[채용수집] 인디스워크 상세 수집 실패 {}: {}", entry.url(), e.getMessage());
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("[채용수집] 인디스워크 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }

    Optional<JobSearchResult> parseArticle(String html, String url) {
        if (html == null || html.isBlank()) return Optional.empty();
        Document document = Jsoup.parse(html, url);
        String bodyText = document.body().text();
        if (bodyText.contains("이 공고는 마감된 공고 혹은 비공개입니다")) return Optional.empty();

        Element titleMeta = document.selectFirst("meta[property=og:title]");
        String rawTitle = titleMeta == null ? null : titleMeta.attr("content").trim();
        if (rawTitle == null || rawTitle.isBlank()) return Optional.empty();
        rawTitle = rawTitle.replaceFirst("\\s*[–-]\\s*IN THIS WORK.*$", "").trim();

        int divider = rawTitle.indexOf('｜');
        if (divider < 0) divider = rawTitle.indexOf('|');
        if (divider <= 0 || divider >= rawTitle.length() - 1) return Optional.empty();
        String company = rawTitle.substring(0, divider).trim();
        String title = rawTitle.substring(divider + 1).trim();

        String jobType = labeledParagraph(document, "고용형태");
        String location = labeledParagraph(document, "근무지");
        if (location != null && location.startsWith(company)) {
            location = location.substring(company.length()).trim();
        }

        return Optional.of(new JobSearchResult(
                lastPathSegment(url), title, company, location, url,
                deadline(document), jobType, null, source()));
    }

    private String labeledParagraph(Document document, String label) {
        for (Element strong : document.select("strong")) {
            if (!label.equals(strong.text().trim())) continue;
            Element parent = strong.closest("p");
            if (parent == null) continue;
            String value = parent.text().replaceFirst("^" + Pattern.quote(label) + "\\s*", "").trim();
            return value.isBlank() ? null : value;
        }
        return null;
    }

    private LocalDate deadline(Document document) {
        for (String label : List.of("마감일", "접수기간")) {
            String value = labeledParagraph(document, label);
            if (value == null) continue;
            Matcher matcher = DATE.matcher(value);
            LocalDate last = null;
            while (matcher.find()) {
                try {
                    last = LocalDate.of(Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
                } catch (Exception ignored) {
                    // Continue to the next date in the range.
                }
            }
            if (last != null) return last;
        }
        return null;
    }

    private String lastPathSegment(String url) {
        try {
            String[] parts = URI.create(url).getPath().split("/");
            return parts.length == 0 ? null : parts[parts.length - 1];
        } catch (Exception ignored) {
            return null;
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
