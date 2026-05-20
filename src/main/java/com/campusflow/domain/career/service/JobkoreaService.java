package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobkoreaService {

    private static final String API_URL = "https://www.jobkorea.co.kr/Search/api/display/v2/jobs";

    private final ObjectMapper objectMapper;

    public List<JobSearchResult> searchJobs(String keyword, int page,
                                             String region, String career, String empType) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("pageSize", 20);
            body.put("pageNumber", page);
            body.put("sortType", "1");
            body.put("sortOrder", "DESC");
            body.put("keyword", keyword);
            body.put("clientType", "PC");
            body.put("jobClassificationCodeList", List.of());
            body.put("areaCodeList", toJobkoreaAreaCode(region));
            body.put("industryCodeList", List.of());
            body.put("employmentTypeCodeList", toJobkoreaEmpType(empType));
            body.put("careerCodeList", toJobkoreaCareer(career));
            body.put("educationCodeList", List.of());
            body.put("companyTypeCodeList", List.of());
            body.put("benefitCodeList", List.of());
            body.put("salaryCode", "");
            body.put("registPeriod", "");
            body.put("minSalary", 0);
            body.put("maxSalary", 0);
            body.put("companyName", "");
            body.put("recruitTitle", "");

            String response = RestClient.create().post()
                    .uri(API_URL)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.jobkorea.co.kr/Search?stext=" + keyword + "&tabType=recruit")
                    .header("Origin", "https://www.jobkorea.co.kr")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("잡코리아 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<JobSearchResult> parseResponse(String json) throws Exception {
        List<JobSearchResult> results = new ArrayList<>();
        JsonNode root = objectMapper.readTree(json);
        JsonNode content = root.get("content");
        if (content == null || !content.isArray()) return results;

        for (JsonNode job : content) {
            String createdAt = job.path("createdAt").asText("");
            LocalDate deadline = null;
            if (createdAt.length() >= 10) {
                try { deadline = LocalDate.parse(createdAt.substring(0, 10)); } catch (DateTimeParseException ignored) {}
            }

            String jobNo = job.path("legacyJobNo").asText();
            String careerCode = job.path("careerType").asText("");
            String careerLabel = switch (careerCode) {
                case "0" -> "경력무관";
                case "1" -> "신입";
                case "2" -> "경력";
                default -> "";
            };

            results.add(new JobSearchResult(
                    jobNo,
                    job.path("title").asText(),
                    job.path("companyName").asText(),
                    extractArea(job),
                    "https://www.jobkorea.co.kr/Recruit/GI_Read/" + jobNo,
                    deadline,
                    careerLabel,
                    job.path("payRange").asText(null),
                    "잡코리아"
            ));
        }
        return results;
    }

    private List<Map<String, Object>> toJobkoreaAreaCode(String region) {
        if (region == null || region.isBlank()) return List.of();
        Map<String, String> codes = Map.of(
            "서울", "101000", "경기", "120000", "인천", "102000",
            "부산", "103000", "대구", "104000", "대전", "106000",
            "광주", "105000", "울산", "107000", "세종", "130000"
        );
        String code = codes.get(region);
        if (code == null) return List.of();
        Map<String, Object> item = new HashMap<>();
        item.put("code", code);
        item.put("name", region);
        return List.of(item);
    }

    private List<String> toJobkoreaCareer(String career) {
        if (career == null || career.isBlank()) return List.of();
        return switch (career) {
            case "신입" -> List.of("1");
            case "경력" -> List.of("2");
            default -> List.of();
        };
    }

    private List<String> toJobkoreaEmpType(String empType) {
        if (empType == null || empType.isBlank()) return List.of();
        return switch (empType) {
            case "정규직" -> List.of("1");
            case "계약직" -> List.of("2");
            default -> List.of();
        };
    }

    private String extractArea(JsonNode job) {
        JsonNode areas = job.get("areaCodeList");
        if (areas == null || !areas.isArray() || areas.isEmpty()) return null;
        List<String> areaNames = new ArrayList<>();
        for (JsonNode a : areas) {
            String name = a.path("name").asText("");
            if (!name.isBlank()) areaNames.add(name);
        }
        return areaNames.isEmpty() ? null : String.join(", ", areaNames);
    }
}
