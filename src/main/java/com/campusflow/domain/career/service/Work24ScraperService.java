package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 고용24 (work24.go.kr) 채용정보 스크래퍼
 * - GET으로 쿠키 획득 → POST로 검색 결과 파싱
 * - API 키 불필요
 */
@Slf4j
@Service
public class Work24ScraperService {

    private static final String BASE = "https://www.work24.go.kr";
    private static final String LIST_URL  = BASE + "/wk/a/b/1200/retriveDtlEmpSrchListInPost.do";
    private static final String INIT_URL  = BASE + "/wk/a/b/1200/retriveDtlEmpSrchList.do";
    private static final String DETAIL_URL = BASE + "/wk/a/b/1200/retriveDtlEmpSrchList.do?wantedAuthNo=";
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<JobSearchResult> searchJobs(String keyword, String region,
                                             String career, String empType, int page) {
        try {
            String cookie = fetchCookie();
            HttpHeaders headers = buildHeaders(cookie);
            MultiValueMap<String, String> body = buildBody(keyword, region, career, empType, page);

            String html = RestClient.create().post()
                    .uri(LIST_URL)
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parse(html);
        } catch (Exception e) {
            log.error("고용24 스크래핑 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String fetchCookie() {
        try {
            ResponseEntity<String> response = RestClient.create().get()
                    .uri(INIT_URL)
                    .header("User-Agent", userAgent())
                    .retrieve()
                    .toEntity(String.class);
            List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies == null) return "";
            return String.join("; ", setCookies.stream().map(c -> c.split(";")[0]).toList());
        } catch (Exception e) {
            log.warn("고용24 쿠키 획득 실패: {}", e.getMessage());
            return "";
        }
    }

    private HttpHeaders buildHeaders(String cookie) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.set("User-Agent", userAgent());
        h.set("Referer", INIT_URL);
        h.set("Origin", BASE);
        if (!cookie.isBlank()) h.set("Cookie", cookie);
        return h;
    }

    private MultiValueMap<String, String> buildBody(String keyword, String region,
                                                      String career, String empType, int page) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("searchMode", "Y");
        body.add("siteClcd", "all");
        body.add("empTpGbcd", "1");
        body.add("sortField", "DATE");
        body.add("sortOrderBy", "DESC");
        body.add("resultCnt", "20");
        body.add("currentPageNo", String.valueOf(page));
        body.add("pageIndex", String.valueOf(page));
        body.add("keyword", keyword != null ? keyword : "");
        body.add("occupation", "09");
        body.add("benefitSrchAndOr", "O");
        body.add("academicGbnoEdu", "noEdu");

        String regionCode = toWork24RegionCode(region);
        body.add("region", regionCode);
        if (!regionCode.isBlank()) {
            body.add("codeDepth1Info", regionCode);
            body.add("codeDepth2Info", regionCode);
        }

        if (career != null) body.add("careerTypes", switch (career) {
            case "신입" -> "1";
            case "경력" -> "2";
            default -> "";
        });

        if (empType != null) body.add("employGbn", switch (empType) {
            case "정규직" -> "10";
            case "계약직" -> "20";
            default -> "";
        });

        return body;
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
        if (table == null) return results;

        for (Element row : table.select("tbody tr")) {
            Elements links = row.select("a");
            if (links.size() < 2) continue;

            String company = links.get(0).text().trim();
            String title   = links.get(1).text().trim();
            String href    = links.get(1).attr("href");

            Matcher m = Pattern.compile("wantedAuthNo=([^&'\"]+)").matcher(href);
            String jobId = m.find() ? m.group(1) : "";

            Elements cells = row.select("td");
            String ddayText = cells.isEmpty() ? "" : cells.last().text();
            LocalDate deadline = null;
            Matcher dm = DATE_PATTERN.matcher(ddayText);
            if (dm.find()) {
                try { deadline = LocalDate.parse(dm.group(1), DATE_FMT); } catch (Exception ignored) {}
            }

            results.add(new JobSearchResult(
                    jobId, title, company, extractLocation(row),
                    DETAIL_URL + jobId, deadline, null, null, "고용24"
            ));
        }
        return results;
    }

    private String extractLocation(Element row) {
        Element cell = row.selectFirst("td:nth-child(2)");
        if (cell == null) return null;
        for (Element p : cell.select("p")) {
            String t = p.text().trim();
            if (!t.isBlank() && !p.hasClass("vline_group")) return t;
        }
        return null;
    }

    private String userAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    }
}
