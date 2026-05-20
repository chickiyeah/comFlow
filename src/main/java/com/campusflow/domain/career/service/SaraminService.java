package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 사람인 채용공고 검색 서비스
 * API 키 승인 후 saramin.api.key 설정 필요
 * GET https://oapi.saramin.co.kr/job-search?access-key={key}&keywords={keyword}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaraminService {

    private static final String BASE_URL = "https://oapi.saramin.co.kr/job-search";

    @Value("${saramin.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    public List<JobSearchResult> searchJobs(String keyword, int page,
                                             String region, String career, String empType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("사람인 API 키 미설정 — 빈 결과 반환 (API 승인 대기 중)");
            return List.of();
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("access-key", apiKey)
                    .queryParam("keywords", keyword)
                    .queryParam("start", page * 20)
                    .queryParam("count", 20)
                    .queryParam("sr", "directhire");

            if (region != null && !region.isBlank()) builder.queryParam("loc_cd", toSaraminRegion(region));
            if (career != null && !career.isBlank()) builder.queryParam("exp_cd", toSaraminCareer(career));
            if (empType != null && !empType.isBlank()) builder.queryParam("job_type", toSaraminEmpType(empType));

            String json = RestClient.create().get()
                    .uri(builder.build(false).encode().toUriString())
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            return parseResponse(json);
        } catch (Exception e) {
            log.error("사람인 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<JobSearchResult> parseResponse(String json) throws Exception {
        List<JobSearchResult> results = new ArrayList<>();
        JsonNode root = objectMapper.readTree(json);
        JsonNode jobs = root.path("jobs").path("job");
        if (!jobs.isArray()) return results;

        for (JsonNode job : jobs) {
            String id = job.path("id").asText();
            String title = job.path("position").path("title").asText();
            String company = job.path("company").path("detail").path("name").asText();
            String location = job.path("position").path("location").path("name").asText(null);
            String url = job.path("url").asText(null);
            String careerLabel = job.path("position").path("experience-level").path("name").asText(null);
            String salary = job.path("salary").path("name").asText(null);

            long expTimestamp = job.path("expiration-timestamp").asLong(0);
            LocalDate deadline = expTimestamp > 0
                    ? Instant.ofEpochSecond(expTimestamp).atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
                    : null;

            results.add(new JobSearchResult(id, title, company, location, url, deadline, careerLabel, salary, "사람인"));
        }
        return results;
    }

    private String toSaraminRegion(String region) {
        return switch (region) {
            case "서울" -> "101000";
            case "경기" -> "102000";
            case "인천" -> "108000";
            case "부산" -> "106000";
            case "대구" -> "104000";
            case "대전" -> "103000";
            case "광주" -> "105000";
            case "울산" -> "107000";
            default -> "";
        };
    }

    private String toSaraminCareer(String career) {
        return switch (career) {
            case "신입" -> "1";
            case "경력" -> "3";
            default -> "";
        };
    }

    private String toSaraminEmpType(String empType) {
        return switch (empType) {
            case "정규직" -> "1";
            case "계약직" -> "2";
            default -> "";
        };
    }
}
