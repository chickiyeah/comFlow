package com.campusflow.domain.portfolio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitHubService {

    private static final String API_BASE = "https://api.github.com";
    private static final Pattern REPO_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/.]+)");

    // 읽을 가치 있는 파일 우선순위 패턴 (낮을수록 우선)
    private static final List<String> PRIORITY_PATTERNS = List.of(
            "main", "app", "index", "server", "application",   // 진입점
            "router", "routes", "api", "controller", "handler", // API
            "service", "usecase", "domain",                      // 비즈니스 로직
            "model", "entity", "schema", "types"                 // 데이터 모델
    );

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".jsx", ".tsx",
            ".go", ".rb", ".php", ".kt", ".cs", ".rs", ".swift"
    );

    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", ".git", "dist", "build", "out", "target",
            "vendor", "__pycache__", ".next", "coverage", "test", "tests",
            "spec", "specs", "__tests__", ".gradle", "gradle"
    );

    public String extractRepoContext(String githubUrl) {
        Matcher m = REPO_PATTERN.matcher(githubUrl);
        if (!m.find()) throw new IllegalArgumentException("GitHub URL 파싱 실패");

        String owner = m.group(1);
        String repo  = m.group(2);

        RestClient client = RestClient.builder()
                .baseUrl(API_BASE)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "CampusFlow-App")
                .build();

        StringBuilder ctx = new StringBuilder();
        ctx.append("=== GitHub 저장소: ").append(owner).append("/").append(repo).append(" ===\n");

        appendRepoInfo(client, owner, repo, githubUrl, ctx);
        appendLanguages(client, owner, repo, ctx);

        // 파일 트리 가져와서 중요 파일 선별
        String defaultBranch = getDefaultBranch(client, owner, repo);
        List<String> codeFiles = getImportantFiles(client, owner, repo, defaultBranch);

        // README
        appendReadme(client, owner, repo, ctx);

        // 실제 코드 파일 내용 읽기 (총 6000자 제한)
        if (!codeFiles.isEmpty()) {
            ctx.append("\n=== 소스 코드 분석 ===\n");
            int totalChars = 0;
            for (String path : codeFiles) {
                if (totalChars >= 6000) break;
                String content = readFileContent(client, owner, repo, path);
                if (content == null || content.isBlank()) continue;

                int remaining = 6000 - totalChars;
                String trimmed = content.length() > remaining ? content.substring(0, remaining) + "\n..." : content;
                ctx.append("\n--- ").append(path).append(" ---\n").append(trimmed).append("\n");
                totalChars += trimmed.length();
            }
        }

        return ctx.toString();
    }

    private String getDefaultBranch(RestClient client, String owner, String repo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> info = client.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve().body(Map.class);
            if (info != null && info.get("default_branch") instanceof String b) return b;
        } catch (RestClientException e) { /* ignore */ }
        return "main";
    }

    @SuppressWarnings("unchecked")
    private List<String> getImportantFiles(RestClient client, String owner, String repo, String branch) {
        try {
            Map<String, Object> tree = client.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branch)
                    .retrieve().body(Map.class);
            if (tree == null) return List.of();

            List<Map<String, Object>> items = (List<Map<String, Object>>) tree.get("tree");
            if (items == null) return List.of();

            // 파일만 필터링 (blob = 파일, tree = 디렉토리)
            List<String> candidates = items.stream()
                    .filter(i -> "blob".equals(i.get("type")))
                    .map(i -> (String) i.get("path"))
                    .filter(Objects::nonNull)
                    .filter(this::isCodeFile)
                    .filter(p -> !isSkippedPath(p))
                    .collect(Collectors.toList());

            // 우선순위 기반 정렬
            candidates.sort(Comparator.comparingInt(this::filePriority));

            // 상위 20개만
            return candidates.stream().limit(20).toList();
        } catch (RestClientException e) {
            log.warn("파일 트리 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isCodeFile(String path) {
        String lower = path.toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private boolean isSkippedPath(String path) {
        String[] parts = path.split("/");
        return Arrays.stream(parts).anyMatch(p -> SKIP_DIRS.contains(p.toLowerCase()));
    }

    private int filePriority(String path) {
        String name = path.toLowerCase().replaceAll("\\.[^.]+$", "");
        String[] parts = name.split("/");
        String filename = parts[parts.length - 1];

        // 파일명으로 우선순위
        for (int i = 0; i < PRIORITY_PATTERNS.size(); i++) {
            if (filename.contains(PRIORITY_PATTERNS.get(i))) return i;
        }

        // 디렉토리 깊이가 낮을수록 우선
        int depth = parts.length;
        return PRIORITY_PATTERNS.size() + depth;
    }

    @SuppressWarnings("unchecked")
    private String readFileContent(RestClient client, String owner, String repo, String path) {
        try {
            Map<String, Object> file = client.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve().body(Map.class);
            if (file == null) return null;

            String encoded = (String) file.get("content");
            if (encoded == null) return null;

            return new String(Base64.getMimeDecoder().decode(encoded));
        } catch (RestClientException e) {
            log.warn("파일 내용 조회 실패 ({}): {}", path, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void appendRepoInfo(RestClient client, String owner, String repo,
                                 String githubUrl, StringBuilder ctx) {
        try {
            Map<String, Object> info = client.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve().body(Map.class);
            if (info == null) return;

            ctx.append("저장소 이름: ").append(info.getOrDefault("name", "")).append("\n");
            ctx.append("설명: ").append(info.getOrDefault("description", "없음")).append("\n");
            ctx.append("URL: ").append(githubUrl).append("\n");

            Object createdAt = info.get("created_at");
            if (createdAt != null) ctx.append("생성일: ").append(createdAt.toString(), 0, 10).append("\n");

            Object topics = info.get("topics");
            if (topics instanceof Iterable<?> t) {
                ctx.append("토픽: ");
                t.forEach(tp -> ctx.append(tp).append(", "));
                ctx.append("\n");
            }
        } catch (RestClientException e) {
            log.warn("저장소 정보 조회 실패: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendLanguages(RestClient client, String owner, String repo, StringBuilder ctx) {
        try {
            Map<String, Object> langs = client.get()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve().body(Map.class);
            if (langs != null && !langs.isEmpty())
                ctx.append("사용 언어: ").append(String.join(", ", langs.keySet())).append("\n");
        } catch (RestClientException e) {
            log.warn("언어 정보 조회 실패: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendReadme(RestClient client, String owner, String repo, StringBuilder ctx) {
        try {
            Map<String, Object> readme = client.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .retrieve().body(Map.class);
            if (readme == null) return;

            String encoded = (String) readme.get("content");
            if (encoded != null) {
                String decoded = new String(Base64.getMimeDecoder().decode(encoded));
                String trimmed = decoded.length() > 2000 ? decoded.substring(0, 2000) + "..." : decoded;
                ctx.append("\n=== README ===\n").append(trimmed).append("\n");
            }
        } catch (RestClientException e) {
            log.warn("README 조회 실패: {}", e.getMessage());
        }
    }
}
