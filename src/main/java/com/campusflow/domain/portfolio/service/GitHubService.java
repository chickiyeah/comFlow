package com.campusflow.domain.portfolio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GitHubService {

    private static final String API_BASE = "https://api.github.com";
    private static final Pattern REPO_PATTERN =
            Pattern.compile("github\\.com/([^/]+)/([^/^.]+)");

    public String extractRepoContext(String githubUrl) {
        Matcher m = REPO_PATTERN.matcher(githubUrl);
        if (!m.find()) {
            throw new IllegalArgumentException("GitHub URL에서 저장소 정보를 파싱할 수 없습니다.");
        }
        String owner = m.group(1);
        String repo = m.group(2);

        RestClient client = RestClient.builder()
                .baseUrl(API_BASE)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "CampusFlow-App")
                .build();

        StringBuilder context = new StringBuilder();
        context.append("=== GitHub 저장소: ").append(owner).append("/").append(repo).append(" ===\n");

        // 기본 정보
        appendRepoInfo(client, owner, repo, githubUrl, context);

        // 언어 정보
        appendLanguages(client, owner, repo, context);

        // README
        appendReadme(client, owner, repo, context);

        return context.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendRepoInfo(RestClient client, String owner, String repo,
                                 String githubUrl, StringBuilder context) {
        try {
            Map<String, Object> info = client.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .body(Map.class);
            if (info == null) return;

            context.append("저장소 이름: ").append(info.getOrDefault("name", "")).append("\n");
            context.append("설명: ").append(info.getOrDefault("description", "없음")).append("\n");
            context.append("URL: ").append(githubUrl).append("\n");
            context.append("Star: ").append(info.getOrDefault("stargazers_count", 0)).append("\n");

            Object createdAt = info.get("created_at");
            if (createdAt != null) {
                context.append("생성일: ").append(createdAt.toString(), 0, 10).append("\n");
            }
            Object updatedAt = info.get("updated_at");
            if (updatedAt != null) {
                context.append("최근 업데이트: ").append(updatedAt.toString(), 0, 10).append("\n");
            }

            Object topics = info.get("topics");
            if (topics instanceof Iterable<?> t) {
                context.append("토픽: ");
                t.forEach(tp -> context.append(tp).append(", "));
                context.append("\n");
            }
        } catch (RestClientException e) {
            log.warn("GitHub 저장소 정보 조회 실패: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendLanguages(RestClient client, String owner, String repo, StringBuilder context) {
        try {
            Map<String, Object> langs = client.get()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve()
                    .body(Map.class);
            if (langs != null && !langs.isEmpty()) {
                context.append("사용 언어: ").append(String.join(", ", langs.keySet())).append("\n");
            }
        } catch (RestClientException e) {
            log.warn("GitHub 언어 정보 조회 실패: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendReadme(RestClient client, String owner, String repo, StringBuilder context) {
        try {
            Map<String, Object> readme = client.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .retrieve()
                    .body(Map.class);
            if (readme == null) return;

            String encoded = (String) readme.get("content");
            if (encoded != null) {
                String decoded = new String(Base64.getMimeDecoder().decode(encoded));
                // README가 너무 길면 앞 3000자만 사용
                String trimmed = decoded.length() > 3000 ? decoded.substring(0, 3000) + "..." : decoded;
                context.append("\n=== README ===\n").append(trimmed);
            }
        } catch (RestClientException e) {
            log.warn("GitHub README 조회 실패 (README 없을 수 있음): {}", e.getMessage());
        }
    }
}
