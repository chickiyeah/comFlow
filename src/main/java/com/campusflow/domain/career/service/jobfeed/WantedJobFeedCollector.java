package com.campusflow.domain.career.service.jobfeed;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WantedJobFeedCollector implements JobFeedCollector {

    static final String LIST_URL = "https://www.wanted.co.kr/api/v4/jobs"
            + "?country=kr&job_sort=job.latest_order&locations=all&years=-1&limit=";
    private static final int DEFAULT_LIMIT = 30;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 Chrome/126.0 Safari/537.36 CampusFlowJobCollector/1.0";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final int limit;

    @Autowired
    public WantedJobFeedCollector(ObjectMapper objectMapper) {
        this(defaultClient(), objectMapper, DEFAULT_LIMIT);
    }

    WantedJobFeedCollector(RestClient restClient, ObjectMapper objectMapper, int limit) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.limit = limit;
    }

    @Override
    public String source() {
        return "원티드";
    }

    @Override
    public List<JobSearchResult> collectLatest() {
        try {
            String json = restClient.get().uri(LIST_URL + limit + "&offset=0")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.wanted.co.kr/wdlist")
                    .header("Accept", "application/json")
                    .retrieve().body(String.class);
            return parseList(json);
        } catch (Exception e) {
            log.warn("[채용수집] 원티드 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }

    List<JobSearchResult> parseList(String json) throws Exception {
        JsonNode data = objectMapper.readTree(json).path("data");
        if (!data.isArray()) return List.of();

        List<JobSearchResult> results = new ArrayList<>();
        for (JsonNode node : data) {
            if (!"active".equalsIgnoreCase(node.path("status").asText())
                    || node.path("hidden").asBoolean(false)) continue;
            String id = text(node, "id");
            String title = text(node, "position");
            String company = text(node.path("company"), "name");
            if (id == null || title == null || company == null) continue;

            results.add(new JobSearchResult(
                    id,
                    title,
                    company,
                    location(node.path("address")),
                    "https://www.wanted.co.kr/wd/" + id,
                    date(text(node, "due_time")),
                    experience(node),
                    null,
                    source()
            ));
        }
        return results;
    }

    private String experience(JsonNode node) {
        int from = node.path("annual_from").asInt(-1);
        int to = node.path("annual_to").asInt(-1);
        if (from == 0 && to == 0) return "신입";
        if (from >= 0 && to >= from) return "경력 " + from + "~" + to + "년";
        if (from > 0) return "경력 " + from + "년 이상";
        return null;
    }

    private String location(JsonNode address) {
        String full = text(address, "full_location");
        if (full != null) return full;
        String city = text(address, "location");
        String district = text(address, "district");
        if (city == null) return district;
        return district == null ? city : city + " " + district;
    }

    private LocalDate date(String value) {
        if (value == null || value.length() < 10) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static RestClient defaultClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(12_000);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
