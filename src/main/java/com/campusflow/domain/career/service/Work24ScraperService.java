package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 고용24 (work24.go.kr) 채용정보 스크래퍼.
 *
 * 2026-06-29 갱신: work24가 검색 흐름을 변경 — 구 POST(`retriveDtlEmpSrchListInPost.do`)는
 * 302로 쿼리스트링 GET(`retriveDtlEmpSrchList.do?...`)로 리다이렉트만 한다. RestClient가
 * 이 POST→GET 리다이렉트를 제대로 못 따라가 에러페이지를 받아 0건이던 문제를, **쿼리스트링
 * GET 직접 호출**로 전환해 해결. 상세 링크도 `empDetailAuthView.do?wantedAuthNo=...`로 변경됨.
 * API 키 불필요.
 */
@Slf4j
@Service
public class Work24ScraperService {

    private static final String BASE = "https://www.work24.go.kr";
    private static final String LIST_URL = BASE + "/wk/a/b/1200/retriveDtlEmpSrchList.do";
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern AUTHNO_PATTERN = Pattern.compile("wantedAuthNo=([^&'\"]+)");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<JobSearchResult> searchJobs(String keyword, String region,
                                             String career, String empType, int page) {
        try {
            String html = RestClient.create().get()
                    .uri(URI.create(buildSearchUrl(keyword, region, career, empType, page)))
                    .header("User-Agent", userAgent())
                    .header("Referer", LIST_URL)
                    .retrieve()
                    .body(String.class);
            return parse(html);
        } catch (Exception e) {
            log.error("고용24 스크래핑 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /** 검색 GET URL 구성 (한글 키워드는 URL 인코딩 — 미인코딩 시 요청 실패). */
    private String buildSearchUrl(String keyword, String region, String career, String empType, int page) {
        StringBuilder sb = new StringBuilder(LIST_URL)
                .append("?searchMode=Y&siteClcd=all&empTpGbcd=1")
                .append("&sortField=DATE&sortOrderBy=DESC&resultCnt=20")
                .append("&currentPageNo=").append(page)
                .append("&pageIndex=").append(page)
                .append("&occupation=09&benefitSrchAndOr=O&academicGbnoEdu=noEdu")
                .append("&keyword=")
                .append(URLEncoder.encode(keyword != null ? keyword : "", StandardCharsets.UTF_8));

        String regionCode = toWork24RegionCode(region);
        sb.append("&region=").append(regionCode);
        if (!regionCode.isBlank()) {
            sb.append("&codeDepth1Info=").append(regionCode).append("&codeDepth2Info=").append(regionCode);
        }

        String careerCode = career == null ? "" : switch (career) {
            case "신입" -> "1";
            case "경력" -> "2";
            default -> "";
        };
        if (!careerCode.isBlank()) sb.append("&careerTypes=").append(careerCode);

        String empCode = empType == null ? "" : switch (empType) {
            case "정규직" -> "10";
            case "계약직" -> "20";
            default -> "";
        };
        if (!empCode.isBlank()) sb.append("&employGbn=").append(empCode);

        return sb.toString();
    }

    private String toWork24RegionCode(String region) {
        if (region == null || region.isBlank()) return "";
        return switch (region) {
            case "서울" -> "11000";
            case "경기" -> "31000";
            case "인천" -> "23000";
            case "부산" -> "21000";
            case "대구" -> "22000";
            case "대전" -> "25000";
            case "광주" -> "24000";
            case "울산" -> "26000";
            case "세종" -> "36000";
            case "강원" -> "32000";
            case "충북" -> "33000";
            case "충남" -> "34000";
            case "전북" -> "35000";
            case "전남" -> "46000";
            case "경북" -> "47000";
            case "경남" -> "48000";
            case "제주" -> "50000";
            default -> "";
        };
    }

    private List<JobSearchResult> parse(String html) {
        List<JobSearchResult> results = new ArrayList<>();
        if (html == null || html.isBlank()) return results;

        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table.box_table.type_pd24");
        if (table == null) table = doc.getElementById("contentArea");
        if (table == null) return results;

        for (Element row : table.select("tbody tr")) {
            // 상세 링크: a[href*=wantedAuthNo] (empDetailAuthView.do)
            Element titleLink = row.selectFirst("a[href*=wantedAuthNo]");
            if (titleLink == null) continue;

            String href = titleLink.attr("href");
            Matcher m = AUTHNO_PATTERN.matcher(href);
            String jobId = m.find() ? m.group(1) : "";
            String title = titleLink.text().trim();

            Element companyLink = row.selectFirst("a.cp_name");
            String company = companyLink != null ? companyLink.text().trim() : "";

            // 마감일: '마감' 라벨 뒤 첫 yyyy-MM-dd (등록일보다 앞서 표기됨)
            LocalDate deadline = null;
            String rowText = row.text();
            int markIdx = rowText.indexOf("마감");
            String dateSrc = markIdx >= 0 ? rowText.substring(markIdx) : rowText;
            Matcher dm = DATE_PATTERN.matcher(dateSrc);
            if (dm.find()) {
                try { deadline = LocalDate.parse(dm.group(1), DATE_FMT); } catch (Exception ignored) {}
            }

            Element site = row.selectFirst("li.site");
            String location = site != null ? site.text().trim() : null;

            Element dollar = row.selectFirst("li.dollar");
            String salary = dollar != null ? dollar.text().trim() : null;

            String url = href.startsWith("http") ? href : BASE + href;

            results.add(new JobSearchResult(
                    jobId, title, company, location, url, deadline, null, salary, "고용24"));
        }
        return results;
    }

    private String userAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    }
}
